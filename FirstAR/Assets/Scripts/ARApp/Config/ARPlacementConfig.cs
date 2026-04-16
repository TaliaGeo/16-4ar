using UnityEngine;

/// <summary>
/// App-wide AR placement configuration. Holds constants that were previously
/// scattered as hard-coded values throughout the monolithic script.
/// Create via Assets → Create → AR → Placement Config.
/// </summary>
[CreateAssetMenu(fileName = "ARPlacementConfig", menuName = "AR/Placement Config")]
public class ARPlacementConfig : ScriptableObject
{
    [Header("Placement")]
    [Tooltip("Default uniform scale applied to all newly placed objects.")]
    public float defaultScale = 1f;

    [Tooltip("Minimum pinch-scale factor.")]
    public float minScale = 0.2f;

    [Tooltip("Maximum pinch-scale factor.")]
    public float maxScale = 3f;

    [Tooltip("Y offset above the detected surface (meters). 0 = flush.")]
    public float yOffset = 0f;

    [Tooltip("If true, placed objects face the camera at placement time.")]
    public bool faceCameraOnPlace = true;

    [Tooltip("Additional rotation offset applied to the prefab after face-camera.")]
    public Vector3 prefabRotationOffsetEuler = Vector3.zero;

    [Tooltip("Default pot height in centimeters if catalog entry has no value.")]
    public float defaultPotHeightCm = 30f;

    [Header("Surface Validation")]
    [Tooltip("Minimum detected plane area (sq m) to accept placement.")]
    public float minPlaneAreaSqm = 0.04f;

    [Tooltip("Maximum surface tilt angle (degrees from vertical up) to accept.")]
    public float maxSurfaceAngleDeg = 25f;

    [Header("Overlap")]
    [Tooltip("Minimum gap between placed objects (meters).")]
    public float minGapMetres = 0.01f;

    [Tooltip("Y threshold for anti-stacking detection.")]
    public float stackYBlock = 0.03f;

    [Header("Freeze Tolerances")]
    [Tooltip("Position drift threshold (squared, meters) before re-freezing.")]
    public float freezePosSqrTol = 1e-8f;

    [Tooltip("Rotation drift threshold (degrees) before re-freezing.")]
    public float freezeRotDegTol = 0.01f;

    [Tooltip("Scale drift threshold (squared) before re-freezing.")]
    public float freezeScaleSqrTol = 1e-8f;

    [Header("Performance")]
    [Tooltip("Frames between anchor health checks (5 = check every 5th frame).")]
    public int healthCheckInterval = 5;

    [Header("Input")]
    [Tooltip("Minimum seconds between tap-to-place actions.")]
    public float tapCooldown = 0.22f;

    [Header("Physics")]
    [Tooltip("Dedicated physics layer for placed AR objects. Must be isolated in project settings.")]
    public int potPhysicsLayer = 31;

    [Header("Screenshot")]
    [Tooltip("Folder name under DCIM (Android) or persistentDataPath (Editor).")]
    public string screenshotFolderName = "MyDesigns";
}
