using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

/// <summary>
/// Manages world-space anchor creation, anchor health monitoring,
/// and plane visual hiding.
///
/// Strategy: objects are placed using a world-pose anchor (not
/// plane-attached). This makes them immune to plane merging,
/// subsumption, and removal — the anchor tracks a fixed point
/// in the real world regardless of plane lifecycle.
///
/// AR Foundation 6.x API:
///   trackablesChanged is UnityEvent → AddListener / RemoveListener
///   args.added / args.updated yield ARPlane directly
///   args.removed yields KeyValuePair&lt;TrackableId, ARPlane&gt;
/// </summary>
[DefaultExecutionOrder(-40)]
public class ARAnchorService : MonoBehaviour
{
    [SerializeField] private ARPlaneManager  planeManager;
    [SerializeField] private ARAnchorManager anchorManager;

    private bool _planeVisualsHidden;

    public List<GameObject> Spawned { get; set; }

    // ── Lifecycle ───────────────────────────────────────────────────

    private void OnEnable()
    {
        if (planeManager)
            planeManager.trackablesChanged.AddListener(OnPlanesChanged);
    }

    private void OnDisable()
    {
        if (planeManager)
            planeManager.trackablesChanged.RemoveListener(OnPlanesChanged);
    }

    public void Init(ARPlaneManager pm, ARAnchorManager am)
    {
        planeManager  = pm;
        anchorManager = am;
    }

    // ── Public API ──────────────────────────────────────────────────

    /// <summary>
    /// Create a world-space anchor at the given pose.
    /// NOT plane-attached — the anchor survives plane removal, merging,
    /// and subsumption without any coordinate-frame shift.
    /// The plane parameter is only used for validation (tracking state).
    /// Returns null on failure.
    /// </summary>
    public ARAnchor TryCreateWorldAnchor(Pose pose, ARPlane plane)
    {
        if (anchorManager == null) return null;
        if (plane != null && plane.trackingState == TrackingState.None) return null;

        var anchorGO = new GameObject("WorldAnchor");
        anchorGO.transform.SetPositionAndRotation(pose.position, pose.rotation);
        ARAnchor anchor = anchorGO.AddComponent<ARAnchor>();

        if (anchor == null)
        {
            Object.Destroy(anchorGO);
            return null;
        }

        Debug.Log($"[ARAnchorService] World anchor created at {pose.position}");
        return anchor;
    }

    /// <summary>
    /// After the first placement, hide existing and future plane visuals.
    /// Plane detection stays ON so new surfaces are found for future placements.
    /// </summary>
    public void DisablePlaneVisualsAfterPlacement()
    {
        if (_planeVisualsHidden) return;
        _planeVisualsHidden = true;
        if (planeManager == null) return;

        foreach (var plane in planeManager.trackables)
            HidePlaneVisual(plane.gameObject);

        if (planeManager.planePrefab != null)
            HidePlaneVisual(planeManager.planePrefab);

        Debug.Log("[ARAnchorService] Plane visuals disabled after first placement");
    }

    /// <summary>
    /// Visibility authority for placed objects. Checks AR session state
    /// AND each anchor's tracking state. Throttled to every
    /// <paramref name="interval"/> frames, or on session state transition.
    /// </summary>
    public void MonitorAnchorHealth(
        ref int frameCounter, int interval, ref ARSessionState lastState)
    {
        if (Spawned == null) return;
        ARSessionState cur = ARSession.state;
        bool changed = cur != lastState;
        lastState = cur;

        if (!changed)
        {
            if (++frameCounter < interval) return;
        }
        frameCounter = 0;

        bool sessionOK = cur == ARSessionState.SessionTracking;

        for (int i = 0; i < Spawned.Count; i++)
        {
            var obj = Spawned[i];
            if (!obj) continue;
            var ctrl = obj.GetComponent<ARObjectController>();
            if (ctrl == null || ctrl.AnchorObject == null) continue;

            var anchor = ctrl.AnchorObject.GetComponent<ARAnchor>();
            if (anchor == null) continue;

            bool visible = sessionOK && anchor.trackingState != TrackingState.None;
            ctrl.SetVisible(visible);
        }
    }

    // ── Plane events ────────────────────────────────────────────────

    private void OnPlanesChanged(ARTrackablesChangedEventArgs<ARPlane> args)
    {
        if (!_planeVisualsHidden) return;
        foreach (var added in args.added)
            HidePlaneVisual(added.gameObject);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static void HidePlaneVisual(GameObject planeGO)
    {
        foreach (var r in planeGO.GetComponentsInChildren<Renderer>(true))
            if (r) r.enabled = false;
    }
}
