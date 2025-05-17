package game

import lib.*

class Post(private val scene: Scene, private val camera: Camera) {
    // private val effect: LensRenderingPipeline
    private val pipeline: DefaultRenderingPipeline
    private var ssao: SSAO2RenderingPipeline? = null
    private var colorCorrection: ColorCorrectionPostProcess? = null

    // Track the current sampling mode
    private var useLinearSampling: Boolean = false

    var depthOfFieldEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                pipeline.depthOfFieldEnabled = value
            }
        }

    var ssaoEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    enableSsao()
                } else {
                    disableSsao()
                }
            }
        }

    var bloomEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                pipeline.bloomEnabled = value
                if (value) {
                    pipeline.bloomThreshold = .5f
                    pipeline.bloomWeight = 0.75f
                    pipeline.bloomKernel = 64f
                    pipeline.bloomScale = 1f
                }
            }
        }

    var sharpenEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                pipeline.sharpenEnabled = value
                if (value) {
                    pipeline.sharpen.edgeAmount = .25f
                    pipeline.sharpen.colorAmount = 1f
                }
            }
        }

    var colorCorrectionEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    enableColorCorrection()
                } else {
                    disableColorCorrection()
                }
            }
        }

    init {
        val pipeline = DefaultRenderingPipeline(
            "defaultPipeline",
            true,
            scene,
            arrayOf(scene.activeCamera!!)
        )

        // Configure depth of field (default: enabled)
        pipeline.depthOfFieldEnabled = depthOfFieldEnabled
        pipeline.depthOfField.fStop = 2f
        pipeline.depthOfField.focalLength = 110f // todo divide by pixel size
        pipeline.depthOfField.focusDistance = 1000f
        pipeline.depthOfFieldBlurLevel = DepthOfFieldEffectBlurLevel.High

        pipeline.hdrEnabled = true

        this.pipeline = pipeline
    }

    private fun enableSsao() {
        if (ssao == null) {
            // Create options object dynamically
            val options = js("({})")
            options.ssaoRatio = 0.5
            options.blurRatio = 0.5

            // Cast camera to ArcRotateCamera
            val arcCamera = scene.activeCamera as ArcRotateCamera

            ssao = SSAO2RenderingPipeline("ssaopipeline", scene, options, arrayOf(arcCamera))
            ssao?.apply {
                radius = 5f
                totalStrength = 0.5f
                samples = 16
                bypassBlur = true
                bilateralSamples = 16
                maxZ = arcCamera.maxZ / 2f
            }
        }
    }

    private fun disableSsao() {
        // Use asDynamic to call dispose
        ssao?.asDynamic()?.dispose(true)
        ssao = null
    }

    private fun enableColorCorrection() {
        if (colorCorrection == null) {
            // Create the color correction post process
            colorCorrection = ColorCorrectionPostProcess(
                name = "Color Correction",
                colorTableUrl = "/assets/lut.png",
                ratio = 1f,
                camera = scene.activeCamera!!
            )
        }
    }

    private fun disableColorCorrection() {
        colorCorrection?.dispose()
        colorCorrection = null
    }

    /**
     * Updates the texture sampling mode for all post-processing effects
     * @param linearSampling If true, use NEAREST_SAMPLINGMODE, otherwise use TRILINEAR_SAMPLINGMODE
     */
    fun updateTextureSamplingMode(linearSampling: Boolean) {
        if (useLinearSampling != linearSampling) {
            useLinearSampling = linearSampling
        }
    }

    fun update() {
        if (camera.view == CameraView.Eye) {
            // Cast a ray from the mouse pointer to find the object under it
            val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), scene.activeCamera)
            val pickResult = ray.intersectsMesh(camera.getPick())

            if (pickResult.pickedPoint != null) {
                // Set focus distance to the distance to the picked point
                pipeline.depthOfField.focusDistance = 1000f * Vector3.Distance(
                    scene.activeCamera!!.globalPosition,
                    pickResult.pickedPoint
                )
            } else {
                // Keep current focus
            }
        } else {
            // Default behavior for other camera views
            pipeline.depthOfField.focusDistance = 1000f * Vector3.Distance(
                scene.activeCamera!!.globalPosition,
                (scene.activeCamera!! as ArcRotateCamera).target
            )
        }
    }
}
