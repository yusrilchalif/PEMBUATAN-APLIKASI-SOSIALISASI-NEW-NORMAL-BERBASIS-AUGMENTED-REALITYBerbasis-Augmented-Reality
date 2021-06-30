package com.example.ta_mask_detection.common

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ta_mask_detection.R
import com.example.ta_mask_detection.common.helpers.DisplayRotationHelper
import com.example.ta_mask_detection.common.helpers.SnackbarHelper
import com.example.ta_mask_detection.common.helpers.TrackingStateHelper
import com.example.ta_mask_detection.common.rendering.BackgroundRenderer
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import java.io.IOException
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TestingClass: Fragment(), GLSurfaceView.Renderer {
    private var session: Session? = null
    private var frameLayout: FrameLayout? = null
    private var surfaceView: GLSurfaceView? = null
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private lateinit var trackingStateHelper: TrackingStateHelper

    var faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()

    private var installRequest = false
    private var canRequestDangerousPermission = true
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private val RC_PERMISSION = 1010

    private var augmentedFaceListener: AugmentedFaceListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayRotationHelper = DisplayRotationHelper(context)
        trackingStateHelper = TrackingStateHelper(requireActivity())
        installRequest=false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        frameLayout = inflater.inflate(R.layout.fragment_augmented_face, container, false) as FrameLayout
        surfaceView = frameLayout?.findViewById<View>(R.id.surface_view) as GLSurfaceView
        surfaceView?.let {
            it.preserveEGLContextOnPause = true
            it.setEGLContextClientVersion(2)
            it.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

            it.setRenderer(this)
            it.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            it.setWillNotDraw(false)
        }

        return frameLayout
    }

    override fun onResume() {
        super.onResume()
        if(session == null){
            var exception: Exception? = null
            var message: String? = null
            try{
                val installStatus = ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequest)

                when(installStatus){
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequest = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                    }
                    else -> {
                        println("Undefined Install status")
                    }
                }

                if (ContextCompat.checkSelfPermission(requireActivity(), "android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED) {
                    // Configure session to use front facing camera.
                    val featureSet: EnumSet<Session.Feature> =
                            EnumSet.of(Session.Feature.FRONT_CAMERA)
                    // Create the session.
                    session = Session( /* context= */context, featureSet)
//                    configureSession()
                } else {
                    requestDangerousPermissions()
                }
            }catch (e: UnavailableArcoreNotInstalledException) {
                message = "please install ARCore"
                exception = e
            }catch (e: UnavailableUserDeclinedInstallationException){
                message = "please install ARCore"
                exception = e
            }catch (e: UnavailableApkTooOldException){
                message = "please update ARCore"
                exception = e
            }catch (e: UnavailableSdkTooOldException){
                message = "please update this app"
                exception = e
            }catch (e: UnavailableDeviceNotCompatibleException){
                message = "this device not support ARCore"
                exception = e
            }catch (e: Exception){
                message = "fail create AR session"
            }
            if(message != null){
                messageSnackbarHelper.showError(requireActivity(), message)
                println("Exception creating session: $exception")
                return
            }
        }

        try {
            session?.resume()
        }catch (e: CameraNotAvailableException){
            messageSnackbarHelper.showError(requireActivity(), "Camara no available, restart app")
            session = null
            return
        }

        surfaceView?.onResume()
        displayRotationHelper.onResume()
    }

    fun requestDangerousPermissions(){
        if(!canRequestDangerousPermission){
            return
        }

        canRequestDangerousPermission = false

        val permission: ArrayList<String> = ArrayList()
        val additionalPermission: ArrayList<String> = ArrayList()
        val permissionLength = additionalPermission.size
        for(i in 0 until permissionLength){
            if (ActivityCompat.checkSelfPermission(requireActivity(), additionalPermission[i]) != PackageManager.PERMISSION_GRANTED){
                permission.add(additionalPermission[i])
            }
        }

//        check camera permission
        if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permission.add(Manifest.permission.CAMERA)
        }

        if(!permission.isEmpty()){
            requestPermissions(permission.toArray(arrayOfNulls<String>(permission.size)), RC_PERMISSION)
        }
    }

    public fun setAugmentedFaceListener(listener: AugmentedFaceListener){
        augmentedFaceListener = listener
    }

    private fun getCanRequestDangerousPermissions(): Boolean?{
        return canRequestDangerousPermission
    }

    private fun setCanRequestDangerousPermissions(canRequestDangerousPermission: Boolean?){
        this.canRequestDangerousPermission = canRequestDangerousPermission!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity(), android.R.style.Theme_Material_Dialog_Alert
        )
        builder
                .setTitle("Camera permission required")
                .setMessage("Add camera permission via Settings?")
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    // If Ok was hit, bring up the Settings app.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts(
                            "package",
                            requireActivity().packageName,
                            null
                    )
                    requireActivity().startActivity(intent)
                    setCanRequestDangerousPermissions(true)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnDismissListener {

                    if (!getCanRequestDangerousPermissions()!!) {
                        requireActivity().finish()
                    }
                }
                .show()
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            displayRotationHelper.onPause()
            surfaceView?.onPause()
            session?.pause()
        }
    }

    override fun onDestroy(){
        if(session!=null){
            session?.close()
            session=null
        }
        super.onDestroy()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        session?.let {
            displayRotationHelper.updateSessionIfNeeded(it)

            try {
                it.setCameraTextureName(backgroundRenderer.textureId)

                val frame: Frame = it.update()
                val camera: Camera = frame.camera

                val projectionMatrix = FloatArray(16)
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

                val viewMatrix = FloatArray(16)
                camera.getViewMatrix(viewMatrix, 0)

                val colorCorrectionRgba = FloatArray(4)
                frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

                backgroundRenderer.draw(frame)

                trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
                val faces: Collection<AugmentedFace> = it.getAllTrackables(AugmentedFace::class.java)
                for(face in faces){
                    if(!faceNodeMap.containsKey(face)){
                        val faceNode = AugmentedFaceNode(face, requireContext())
                        augmentedFaceListener?.onFaceAdded(faceNode)
                        faceNodeMap[face] = faceNode
                    }else{
                        faceNodeMap[face]?.let { node ->
                            augmentedFaceListener?.onFaceUpdate(node)
                        }
                    }

                    val iter = faceNodeMap.entries.iterator()
                    while (iter.hasNext()){
                        val entry = iter.next()
                        val faceNode = entry.key
                        if(faceNode.trackingState == TrackingState.STOPPED){
                            iter.remove()
                        }
                    }

                    if(face.trackingState !== TrackingState.TRACKING){
                        break
                    }

                    GLES20.glDepthMask(false)

                    faceNodeMap[face]?.onDraw(projectionMatrix, viewMatrix, colorCorrectionRgba)
                }
            } catch (t: Throwable){
                println("Exception on the OpenGL thread $t")
            }finally {
                GLES20.glDepthMask(true)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChange(width, height)
        GLES20.glViewport(0,0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        try {
            backgroundRenderer.createOnGlThread(context)
        }catch (e: IOException){
            println("Failed to read an asset file $e")
        }
    }

    private fun configureSession(){
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        session?.configure(config)
    }
}