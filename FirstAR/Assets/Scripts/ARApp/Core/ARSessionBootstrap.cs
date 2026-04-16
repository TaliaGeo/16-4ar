using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;
using UnityEngine.InputSystem.EnhancedTouch;

/// <summary>
/// Configures the AR session, plane detection, occlusion, physics layer
/// isolation, and frame pacing on startup. Does NOT own any placement logic.
/// Place on the same GameObject as ARSession or XR Origin.
/// </summary>
[DefaultExecutionOrder(-100)]
public class ARSessionBootstrap : MonoBehaviour
{
    [SerializeField] private ARPlacementConfig config;
    [SerializeField] private ARPlaneManager planeManager;
    [SerializeField] private AROcclusionManager occlusionManager;
    [SerializeField] private Camera arCamera;

    private void Awake()
    {
        // Let the device choose its own frame pacing.
        Application.targetFrameRate = -1;
        QualitySettings.vSyncCount = 1;

        Physics.autoSyncTransforms = false;

        IsolatePhysicsLayer();
        ResolveReferences();
        ConfigurePlaneDetection();
        ConfigureOcclusion();
        EnableEnhancedTouch();
    }

    private void IsolatePhysicsLayer()
    {
        int layer = config != null ? config.potPhysicsLayer : 31;
        for (int i = 0; i < 32; i++)
            Physics.IgnoreLayerCollision(layer, i, true);
    }

    private void ResolveReferences()
    {
        if (!planeManager) planeManager = FindFirstObjectByType<ARPlaneManager>();
        if (!arCamera) arCamera = Camera.main;

        if (!occlusionManager && arCamera)
            occlusionManager = arCamera.GetComponent<AROcclusionManager>();
    }

    private void ConfigurePlaneDetection()
    {
        if (planeManager)
            planeManager.requestedDetectionMode = PlaneDetectionMode.Horizontal;
    }

    private void ConfigureOcclusion()
    {
        if (!occlusionManager) return;

        occlusionManager.requestedEnvironmentDepthMode = EnvironmentDepthMode.Disabled;
        occlusionManager.requestedOcclusionPreferenceMode = OcclusionPreferenceMode.NoOcclusion;
        occlusionManager.enabled = false;
    }

    private static void EnableEnhancedTouch()
    {
        if (!EnhancedTouchSupport.enabled)
            EnhancedTouchSupport.Enable();
    }
}
