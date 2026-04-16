using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

/// <summary>
/// MonoBehaviour attached to every placed AR object. Holds per-object metadata,
/// frozen local transform, anchor reference, and cached renderers.
/// Replaces the old inner PotInfo class — visible in the Inspector.
/// </summary>
public class ARObjectController : MonoBehaviour
{
    // Per-object metadata
    [HideInInspector] public PlantCatalogEntry CatalogEntry;
    [HideInInspector] public float SurfaceY;
    [HideInInspector] public Vector3 NormalizedScale;
    [HideInInspector] public GameObject AnchorObject;
    [HideInInspector] public GameObject MeasureLabel;
    [HideInInspector] public TrackableId SourcePlaneId;

    // Frozen local transform (set once after placement, updated on scale).
    [HideInInspector] public Vector3 FrozenLocalPos;
    [HideInInspector] public Quaternion FrozenLocalRot;
    [HideInInspector] public Vector3 FrozenLocalScale;
    [HideInInspector] public bool IsFrozen;

    // Performance caches
    [HideInInspector] public Renderer[] cachedRenderers;
    [HideInInspector] public bool lastVisible = true;

    /// <summary>Cache renderers once. Call after all visual setup is complete.</summary>
    public void CacheRenderers()
    {
        cachedRenderers = GetComponentsInChildren<Renderer>(true);
    }

    /// <summary>Freeze the current local transform. LateUpdate will enforce it.</summary>
    public void Freeze()
    {
        FrozenLocalPos = transform.localPosition;
        FrozenLocalRot = transform.localRotation;
        FrozenLocalScale = transform.localScale;
        IsFrozen = true;
    }

    /// <summary>Temporarily unfreeze (e.g. during pinch scale).</summary>
    public void Unfreeze()
    {
        IsFrozen = false;
    }

    /// <summary>
    /// Enforce the frozen local transform if drift exceeds tolerance.
    /// Called by the placement controller in LateUpdate.
    /// </summary>
    public bool EnforceFreeze(float posSqrTol, float rotDegTol, float scaleSqrTol)
    {
        if (!IsFrozen) return false;

        bool drifted =
            (transform.localPosition - FrozenLocalPos).sqrMagnitude > posSqrTol
            || Quaternion.Angle(transform.localRotation, FrozenLocalRot) > rotDegTol
            || (transform.localScale - FrozenLocalScale).sqrMagnitude > scaleSqrTol;

        if (drifted)
        {
            transform.localPosition = FrozenLocalPos;
            transform.localRotation = FrozenLocalRot;
            transform.localScale = FrozenLocalScale;
            transform.hasChanged = false;
            return true;
        }
        return false;
    }

    /// <summary>Set renderer visibility using the cached array. Skips if already at target state.</summary>
    public void SetVisible(bool visible)
    {
        if (visible == lastVisible) return;
        lastVisible = visible;
        if (cachedRenderers == null) return;
        for (int i = 0; i < cachedRenderers.Length; i++)
        {
            var r = cachedRenderers[i];
            if (r) r.enabled = visible;
        }
    }
}
