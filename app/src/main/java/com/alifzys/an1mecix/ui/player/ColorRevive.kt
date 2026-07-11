package com.alifzys.an1mecix.ui.player

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * "Renk Canlandırma" — eski/soluk anime renklerini otomatik olarak canlı, yeni gibi gösteren
 * GL efekti. Gerçek bir sinir ağı değil (TV'de gerçek zamanlı ML imkânsız); bunun yerine
 * uzaysal olmayan, piksel-başı akıllı bir renk düzeltmesi yapar:
 *  - siyah/beyaz nokta esnetme (soluk görüntü = düşük dinamik aralık),
 *  - yumuşak kontrast S-eğrisi,
 *  - vibrance (az doygun pikselleri çok, doygunları az artırır → deri tonlarını korur),
 *  - hafif genel doygunluk.
 * [strength] 0..1 (Hafif ≈ 0.5, Güçlü ≈ 1.0).
 */
@UnstableApi
class ColorReviveEffect(private val strength: Float) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        ColorReviveProgram(strength, useHdr)
}

@UnstableApi
private class ColorReviveProgram(
    strength: Float,
    useHdr: Boolean,
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity= */ 1) {

    private val strength = strength.coerceIn(0f, 1f)

    private val glProgram: GlProgram = try {
        GlProgram(VERTEX_SHADER, FRAG_SHADER)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size =
        Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
            glProgram.setFloatUniform("uStrength", strength)
            glProgram.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private companion object {
        const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            varying vec2 vTexCoord;
            void main() {
              gl_Position = aFramePosition;
              vTexCoord = aFramePosition.xy * 0.5 + 0.5;
            }
        """

        const val FRAG_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform float uStrength;
            varying vec2 vTexCoord;

            vec3 saturate3(vec3 c, float s) {
              float l = dot(c, vec3(0.299, 0.587, 0.114));
              return clamp(mix(vec3(l), c, s), 0.0, 1.0);
            }

            void main() {
              vec3 c = texture2D(uTexSampler, vTexCoord).rgb;
              float k = uStrength;

              // 1) Levels: soluk görüntünün siyah/beyaz noktasını esnet.
              float bp = 0.045 * k;
              float wp = 1.0 - 0.035 * k;
              c = clamp((c - bp) / max(wp - bp, 0.001), 0.0, 1.0);

              // 2) Yumuşak kontrast S-eğrisi.
              c = mix(c, c * c * (3.0 - 2.0 * c), 0.35 * k);

              // 3) Vibrance: az doygun pikselleri daha çok artır (deri tonu korunur).
              float mx = max(max(c.r, c.g), c.b);
              float mn = min(min(c.r, c.g), c.b);
              float sat = mx - mn;
              float vib = (1.0 - sat) * (0.55 * k);
              c = saturate3(c, 1.0 + vib);

              // 4) Genel hafif doygunluk + minik parlaklık.
              c = saturate3(c, 1.0 + 0.15 * k);
              c += 0.02 * k;

              gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
            }
        """
    }
}
