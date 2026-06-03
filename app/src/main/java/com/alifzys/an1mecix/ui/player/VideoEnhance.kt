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
 * Player görüntü iyileştirme modları.
 *
 * Animeler en fazla 1080p master'lanır; "4K" her zaman upscale'dir. Burada gerçek
 * detay üretmiyoruz (real-time ML imkansız), client tarafında uzaysal keskinleştirme /
 * hafif upscale uyguluyoruz:
 *  - SHARPEN : kaynak çözünürlükte hafif unsharp-mask keskinleştirme (ucuz).
 *  - ANIME4K : 1.5x upscale + güçlü keskinleştirme + çizgi koyulaştırma (5-tap, dengeli).
 */
enum class VideoEnhance(val pref: String) {
    OFF("off"),
    SHARPEN("sharpen"),
    ANIME4K("anime4k");

    companion object {
        fun from(pref: String?): VideoEnhance =
            entries.firstOrNull { it.pref == pref } ?: OFF
    }
}

/** ExoPlayer.setVideoEffects(...) için GL efekti. */
@UnstableApi
class VideoEnhanceEffect(private val mode: VideoEnhance) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        EnhanceShaderProgram(mode, useHdr)
}

@UnstableApi
private class EnhanceShaderProgram(
    private val mode: VideoEnhance,
    useHdr: Boolean,
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity= */ 1) {

    private val glProgram: GlProgram = try {
        GlProgram(VERTEX_SHADER, if (mode == VideoEnhance.ANIME4K) FRAG_ANIME4K else FRAG_SHARPEN)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    private var inW = 1
    private var inH = 1

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        inW = inputWidth
        inH = inputHeight
        // Anime4K: 1.5x upscale. 2x (4 kat piksel) kasıyordu; 1.5x (2.25 kat) + 5-tap
        // shader ile yük ~3 kat azalır, upscale yine korunur. Keskinlik: upscale yok.
        return if (mode == VideoEnhance.ANIME4K) {
            Size(inputWidth * 3 / 2, inputHeight * 3 / 2)
        } else {
            Size(inputWidth, inputHeight)
        }
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
            glProgram.setFloatsUniform("uTexSize", floatArrayOf(inW.toFloat(), inH.toFloat()))
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

        // Unsharp-mask keskinleştirme. Kaynak çözünürlükte, ucuz.
        const val FRAG_SHARPEN = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexSize;
            varying vec2 vTexCoord;
            void main() {
              vec2 px = 1.0 / uTexSize;
              vec3 c  = texture2D(uTexSampler, vTexCoord).rgb;
              vec3 n  = texture2D(uTexSampler, vTexCoord + vec2(0.0, -px.y)).rgb;
              vec3 s  = texture2D(uTexSampler, vTexCoord + vec2(0.0,  px.y)).rgb;
              vec3 e  = texture2D(uTexSampler, vTexCoord + vec2( px.x, 0.0)).rgb;
              vec3 w  = texture2D(uTexSampler, vTexCoord + vec2(-px.x, 0.0)).rgb;
              vec3 ne = texture2D(uTexSampler, vTexCoord + vec2( px.x, -px.y)).rgb;
              vec3 nw = texture2D(uTexSampler, vTexCoord + vec2(-px.x, -px.y)).rgb;
              vec3 se = texture2D(uTexSampler, vTexCoord + vec2( px.x,  px.y)).rgb;
              vec3 sw = texture2D(uTexSampler, vTexCoord + vec2(-px.x,  px.y)).rgb;
              vec3 blur = (c * 4.0 + (n + s + e + w) * 2.0 + (ne + nw + se + sw)) / 16.0;
              vec3 res = c + (c - blur) * 1.1;
              gl_FragColor = vec4(clamp(res, 0.0, 1.0), 1.0);
            }
        """

        // Anime4K-esinli: 1.5x upscale + güçlü keskinleştirme + çizgi koyulaştırma.
        // 5-tap (haç) kernel — yüksek çözünürlükte 9-tap'in yükü olmadan.
        const val FRAG_ANIME4K = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexSize;
            varying vec2 vTexCoord;
            void main() {
              vec2 px = 1.0 / uTexSize;
              vec3 c = texture2D(uTexSampler, vTexCoord).rgb;
              vec3 n = texture2D(uTexSampler, vTexCoord + vec2(0.0, -px.y)).rgb;
              vec3 s = texture2D(uTexSampler, vTexCoord + vec2(0.0,  px.y)).rgb;
              vec3 e = texture2D(uTexSampler, vTexCoord + vec2( px.x, 0.0)).rgb;
              vec3 w = texture2D(uTexSampler, vTexCoord + vec2(-px.x, 0.0)).rgb;
              vec3 blur = (c + n + s + e + w) * 0.2;
              vec3 res = clamp(c + (c - blur) * 1.9, 0.0, 1.0);
              // Anime çizgileri koyudur: karanlık pikselleri belirginleştir.
              float l = dot(res, vec3(0.299, 0.587, 0.114));
              res *= mix(0.85, 1.0, smoothstep(0.0, 0.30, l));
              gl_FragColor = vec4(clamp(res, 0.0, 1.0), 1.0);
            }
        """
    }
}
