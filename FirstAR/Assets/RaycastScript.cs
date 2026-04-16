using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using TMPro;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.EnhancedTouch;
using UnityEngine.Events;
using UnityEngine.Networking;
using ETouch = UnityEngine.InputSystem.EnhancedTouch.Touch;
using TouchPhaseIS = UnityEngine.InputSystem.TouchPhase;

// ════════════════════════════════════════════════════════════════════
//  ARPlacementManager  –  PRODUCTION BUILD (v31)
//
//  STABILITY GUARANTEES:
//  • Every placed pot is a child of a PLANE-ATTACHED ARAnchor.
//  • After placement, the pot's localPosition/localRotation/localScale
//    are frozen — the anchor alone handles world-space SLAM tracking.
//  • EnforceFrozenTransforms runs in LateUpdate (once per frame).
//  • World position is NEVER overridden — the anchor system handles
//    relocalization, plane refinement, and drift correction naturally.
//  • ALL Rigidbodies, Joints, Cloth, Animators, XRI components are
//    DESTROYED on placed pots so nothing can move them.
//  • Pots on dedicated physics layer 31 (ARPots) — isolated in both
//    the runtime IgnoreLayerCollision AND the project collision matrix.
//  • Overlap / stacking is blocked at placement AND during pinch-scale.
//  • Surface detection: PlaneWithinPolygon (tight) + PlaneWithinBounds
//    (fallback) for fast detection on tables, chairs, floors.
//  • Plane detection locked to HorizontalUp at subsystem level.
//  • XRI Screen Space Ray Interactors disabled scene-wide.
//  • NO ghost/reticle/transparent preview — pots appear solid only.
//
//  Target device: Google Pixel 4 (1080 × 2160), AR Foundation 6.x
// ════════════════════════════════════════════════════════════════════
[RequireComponent(typeof(ARRaycastManager))]
[RequireComponent(typeof(ARPlaneManager))]
[DefaultExecutionOrder(-50)]
public class ARPlacementManager : MonoBehaviour
{
    // ── THEME: Earthy Green Palette (Pinterest-modern) ──────────────
    // Moss #2C3424  Cypress #4C583E  Cedar #959581  Olive #768064  Aloe #DADED8
    private static readonly Color MOSS    = new Color(0.173f, 0.204f, 0.141f);  // darkest
    private static readonly Color CYPRESS = new Color(0.298f, 0.345f, 0.243f);  // primary
    private static readonly Color CEDAR   = new Color(0.584f, 0.584f, 0.506f);  // mid warm
    private static readonly Color OLIVE   = new Color(0.463f, 0.502f, 0.392f);  // accent
    private static readonly Color ALOE    = new Color(0.855f, 0.871f, 0.847f);  // lightest
    // Soft warm variant
    private static readonly Color CREAM   = new Color(0.94f, 0.93f, 0.90f);    // warm off-white
    // Derived tokens
    private static readonly Color COL_BAR      = new Color(MOSS.r, MOSS.g, MOSS.b, 0.92f);
    private static readonly Color COL_MENU_BG  = CREAM;
    private static readonly Color COL_WHITE    = ALOE;
    private static readonly Color COL_DARK     = MOSS;
    private static readonly Color COL_MGREY    = CEDAR;
    private static readonly Color COL_DEL_TINT = new Color(0.78f, 0.32f, 0.28f, 0.80f);
    private static readonly Color COL_RED_BG   = new Color(0.72f, 0.28f, 0.24f);
    private static readonly Color COL_TOAST_BG = new Color(CYPRESS.r, CYPRESS.g, CYPRESS.b, 0.94f);

    // ── STABILITY CONSTANTS (NOT serialized — scene cannot override) ──
    private const float OVERLAP_RADIUS_FACTOR = 1.0f;
    private const float MIN_GAP_METRES        = 0.01f;
    private const float SURFACE_Y_TOLERANCE   = 0.12f;
    private const float STACK_Y_BLOCK         = 0.03f;
    private const float MIN_PLANE_AREA_SQM    = 0.04f;    // ~20 cm × 20 cm — rejects noisy micro-planes
    private const float MAX_SURFACE_ANGLE_DEG = 25f;      // Wide: accept tilted tables/chairs
    private const float TAP_CD                = 0.22f;
    private const float DEFAULT_POT_HEIGHT_CM = 30f;      // Uniform default size for ALL pots
    private const int   ZTEST_LEQUAL          = 4;
    private const int   POT_PHYSICS_LAYER     = 31;       // Isolated layer — zero cross-interaction

    // Tolerance thresholds for local-transform freeze.
    // Only snap back when drift exceeds these — avoids per-frame
    // writes that fight the anchor system's transform updates.
    private const float FREEZE_POS_SQR_TOL   = 0.0001f * 0.0001f; // 0.1 mm
    private const float FREEZE_ROT_DEG_TOL   = 0.01f;              // degrees
    private const float FREEZE_SCALE_SQR_TOL = 0.0001f * 0.0001f;

    // ── INSPECTOR ──────────────────────────────────────────────────
    [Header("AR")]
    [SerializeField] private ARRaycastManager   raycastManager;
    [SerializeField] private ARPlaneManager     planeManager;
    [SerializeField] private ARAnchorManager    anchorManager;
    [SerializeField] private AROcclusionManager occlusionManager;
    [SerializeField] private Camera             arCamera;

    [Header("Pot Prefabs")]
    [SerializeField] private GameObject[] potPrefabs  = new GameObject[2];
    [SerializeField] private string[]     potNames    = new string[] { "Classic Pot", "Modern Pot" };
    [SerializeField] private float[]      potRealHeightCm = new float[] { 30f, 25f };

    [Header("Scale")]
    [SerializeField] private float defaultScale = 1f;
    [SerializeField] private float minScale     = 0.2f;
    [SerializeField] private float maxScale     = 3f;

    [Header("Placement")]
    [SerializeField] private float   yOffset = 0f;
    [SerializeField] private bool    faceCameraOnPlace = true;
    [SerializeField] private Vector3 prefabRotationOffsetEuler = Vector3.zero;

    [Header("Bounds Filter")]
    [SerializeField] private string[] ignoreRendererNameContains =
        new string[] { "ground", "shadow", "plane" };

    [Header("Screenshot")]
    [SerializeField] private string screenshotFolderName = "MyDesigns";
    public UnityEvent<string> onScreenshotSaved;

    // JWT token + API base URL read from Flutter Intent extras
    private string _jwtToken = "";
    private string _apiBaseUrl = "";

    // ── RUNTIME STATE ──────────────────────────────────────────────
    private int  selectedPotIndex;
    private bool deleteMode;
    private readonly List<GameObject> spawned = new List<GameObject>();

    private static readonly List<ARRaycastHit> sHits = new List<ARRaycastHit>();
    private readonly List<RaycastResult> uiHits = new List<RaycastResult>();
    private float lastTapTime = -10f;

    private float      pinchStartDist;
    private Vector3    pinchStartScale;
    private GameObject pinchTarget;

    private GameObject activeLabelPot;

    // Tracks whether we've reduced plane detection after first placement.
    // After the first pot is placed, we stop updating planes to prevent
    // boundary refinement from drifting anchors.
    private bool _planeDetectionReducedAfterPlace;

    private static readonly int ZTestMode = Shader.PropertyToID("unity_GUIZTestMode");

    // ── UI REFS ────────────────────────────────────────────────────
    private Canvas     canvas;
    private GameObject barGO, menuGO, bannerGO, toastGO;
    private TMP_Text   toastTxt;
    private Coroutine  toastCR;
    private Image      sel1Img, sel2Img;
    private Sprite     spr1, spr2;

    // ── RAYCAST FILTERS ──
    // Pass 1: Polygon only — tight accurate boundary, prevents table planes
    //         from extending beyond their real edges and catching floor rays.
    // Pass 2: Bounds fallback — for surfaces without a refined polygon yet.
    private static readonly TrackableType PlaneFilterTight =
        TrackableType.PlaneWithinPolygon;
    private static readonly TrackableType PlaneFilterLoose =
        TrackableType.PlaneWithinBounds;

    // ── PotInfo: per-pot metadata + frozen local transform ──────────
    // After placement, localPosition/localRotation are FROZEN.
    // LateUpdate enforces these values every frame as a safety net
    // against any rogue AR subsystem or stale component touching
    // the transform.  The ANCHOR handles world-space stability;
    // we only protect the LOCAL offset (which should be constant).
    private class PotInfo : MonoBehaviour
    {
        public int        idx;
        public float      surfaceY;
        public Vector3    normScale;
        public GameObject anchorObj;
        public GameObject measureLabel;
        // Frozen local-space transform (set once after placement).
        // Only LOCAL transforms are frozen — the ARAnchor parent
        // handles world-space SLAM tracking and relocalization.
        public Vector3    frozenLocalPos;
        public Quaternion frozenLocalRot;
        public Vector3    frozenLocalScale;
        public bool       isFrozen;
        public TrackableId sourcePlaneId;
        // ── Performance caches ──
        public Renderer[] cachedRenderers;   // set once at spawn
        public bool       lastVisible = true; // avoid redundant enable/disable
    }

    // ── PERFORMANCE STATE ───────────────────────────────────────────
    private int  _healthCheckFrame;               // frame counter for throttled health check
    private const int HEALTH_CHECK_INTERVAL = 5;  // check every N frames
    private ARSessionState _lastSessionState;      // detect session state transitions
    private bool _spawnedDirty;                    // set when spawned list may contain nulls

    // ════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════
    private void Awake()
    {
        // Let ARCore drive frame pacing.  On Pixel 4, ARCore targets
        // 30 Hz tracking with 60 Hz rendering when thermal budget
        // allows.  Hardcoding 60 causes stutter under thermal throttle.
        // Setting -1 removes the cap; Android's Choreographer + ARCore
        // will pace frames to the display refresh rate naturally.
        Application.targetFrameRate = -1;
        QualitySettings.vSyncCount  = 1;   // Sync to display refresh — smooth pacing

        // Prevent Unity physics from touching ANY transform behind our back
        Physics.autoSyncTransforms = false;

        // Isolate pot physics layer — pots cannot interact with ANYTHING
        for (int i = 0; i < 32; i++)
            Physics.IgnoreLayerCollision(POT_PHYSICS_LAYER, i, true);

        // Suppress developer console from ever appearing to users
        SuppressDeveloperMode();

        if (!raycastManager)   raycastManager   = FindFirstObjectByType<ARRaycastManager>();
        if (!planeManager)     planeManager     = FindFirstObjectByType<ARPlaneManager>();
        if (!anchorManager)    anchorManager    = FindFirstObjectByType<ARAnchorManager>();
        if (!occlusionManager) occlusionManager = FindFirstObjectByType<AROcclusionManager>();
        if (!arCamera)         arCamera         = Camera.main;

        if (!anchorManager)
        {
            GameObject host = planeManager ? planeManager.gameObject : gameObject;
            anchorManager = host.AddComponent<ARAnchorManager>();
            Debug.LogWarning("[AR] ARAnchorManager was missing — added at runtime to " + host.name);
        }

        ConfigureOcclusion();
        ConfigurePlaneDetection();
        DisableXRIInteractors();
        ConfigureFeaturePoints();

        EnhancedTouchSupport.Enable();
        ReadIntentExtras();
        LoadPotSprites();
        canvas = CreateCanvas();
        SetupRuntimeUI();
    }

    private void OnEnable()
    {
        if (planeManager != null)
            planeManager.trackablesChanged.AddListener(OnPlanesChanged);
    }

    private void OnDisable()
    {
        if (planeManager != null)
            planeManager.trackablesChanged.RemoveListener(OnPlanesChanged);
    }

    private void Start()
    {
        menuGO.SetActive(false);
        bannerGO.SetActive(false);
        toastGO.SetActive(false);
        CleanPreExistingPots();

        // Reinforce plane detection in case Awake ran before subsystem was ready
        ConfigurePlaneDetection();
        SuppressDeveloperMode();

        Debug.Log($"[AR] Start — anchorManager={anchorManager?.gameObject.name ?? "NULL"}"
            + $", planeManager={planeManager?.gameObject.name ?? "NULL"}"
            + $", occlusionManager={occlusionManager?.gameObject.name ?? "NULL"}"
            + $", camera={arCamera?.name}"
            + $", envDepthMode={occlusionManager?.requestedEnvironmentDepthMode}");
    }

    /// <summary>
    /// Pots persist across app pause/resume.  The ARAnchor system
    /// handles relocalization when the user comes back — pots stay
    /// where they were placed.  Do NOT clear pots here.
    /// </summary>
    private void OnApplicationPause(bool paused)
    {
        // Nothing — pots stay.  AR Foundation will relocalize anchors.
    }

    private void OnApplicationFocus(bool hasFocus)
    {
        // Nothing — pots stay.  AR Foundation will relocalize anchors.
    }

    /// <summary>
    /// Destroy every placed pot and its anchor.  Fresh slate.
    /// </summary>
    private void ClearAllPots()
    {
        for (int i = spawned.Count - 1; i >= 0; i--)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info != null)
            {
                if (info.measureLabel) Destroy(info.measureLabel);
                GameObject toDestroy = info.anchorObj ? info.anchorObj : obj;
                Destroy(toDestroy);
            }
            else
                Destroy(obj);
        }
        spawned.Clear();
        activeLabelPot = null;
        pinchTarget = null;
        SetDeleteMode(false);
    }

    private void Update()
    {
        HandleTouch();
        HandlePinch();
        BillboardLabels();
    }

    private void LateUpdate()
    {
        // Deferred null-cleanup: only when flagged (pot destroyed).
        // Avoids allocating a delegate + scanning the list every frame.
        if (_spawnedDirty)
        {
            spawned.RemoveAll(o => o == null);
            _spawnedDirty = false;
        }

        EnforceFrozenTransforms();
        MonitorAnchorHealth();
    }

    /// <summary>
    /// Enforce frozen LOCAL transforms on every placed pot, once per
    /// frame in LateUpdate.  The ARAnchor parent handles world-space
    /// SLAM tracking — we only protect the local offset from being
    /// modified by rogue components or stale physics.
    ///
    /// ALWAYS runs, even during tracking loss.  If the freeze were
    /// skipped during a tracking outage, a rogue component could move
    /// the local offset and the pot would snap to a wrong position
    /// when tracking recovers.  Visibility is handled separately by
    /// MonitorAnchorHealth (the sole visibility authority).
    /// </summary>
    private void EnforceFrozenTransforms()
    {
        for (int i = 0; i < spawned.Count; i++)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || !info.isFrozen) continue;

            // LOCAL-SPACE FREEZE ONLY (tolerance-gated).
            // The ARAnchor parent tracks the real-world surface via
            // ARCore SLAM.  We never write world position — that would
            // fight the anchor's relocalization and cause jitter.
            // Only snap the local offset back when something has
            // actually moved it beyond the tolerance thresholds.
            Transform t = obj.transform;
            bool drifted =
                (t.localPosition - info.frozenLocalPos).sqrMagnitude > FREEZE_POS_SQR_TOL
             || Quaternion.Angle(t.localRotation, info.frozenLocalRot) > FREEZE_ROT_DEG_TOL
             || (t.localScale - info.frozenLocalScale).sqrMagnitude > FREEZE_SCALE_SQR_TOL;

            if (drifted)
            {
                t.localPosition = info.frozenLocalPos;
                t.localRotation = info.frozenLocalRot;
                t.localScale    = info.frozenLocalScale;
                t.hasChanged    = false;
            }
        }
    }

    /// <summary>Show/hide all renderers on a pot without destroying anything.
    /// Uses cached renderer array to avoid GetComponentsInChildren overhead.</summary>
    private static void SetRenderersVisible(PotInfo info, bool visible)
    {
        if (info.cachedRenderers == null) return;
        for (int i = 0; i < info.cachedRenderers.Length; i++)
        {
            var r = info.cachedRenderers[i];
            if (r) r.enabled = visible;
        }
        info.lastVisible = visible;
    }

    /// <summary>Legacy overload for non-PotInfo objects (plane visuals etc).</summary>
    private static void SetRenderersVisible(GameObject root, bool visible)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (r) r.enabled = visible;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PLANE LIFECYCLE — handle removal, merging, boundary changes
    // ════════════════════════════════════════════════════════════════

    /// <summary>
    /// Called by ARPlaneManager when planes are added, updated, or removed.
    /// When a plane that has a pot anchored to it is REMOVED (often due to
    /// plane merging), we re-anchor the pot to the nearest surviving plane
    /// so it doesn't become orphaned and drift.
    /// </summary>
    private void OnPlanesChanged(ARTrackablesChangedEventArgs<ARPlane> args)
    {
        // Hide visuals on newly added planes if we already placed a pot.
        // DisablePlaneVisualsAfterPlacement only runs once and modifies
        // the prefab, but late-arriving planes from the pool may still
        // have active renderers.
        if (_planeDetectionReducedAfterPlace)
        {
            foreach (var added in args.added)
                HidePlaneVisual(added.gameObject);
        }

        // Handle removed planes — re-anchor any pots that were on them
        foreach (var removed in args.removed)
        {
            ReAnchorPotsOnRemovedPlane(removed.Key);
        }

        // Handle plane MERGES (subsumption).  When ARCore merges two
        // planes the old one isn't always "removed" — it may appear in
        // args.updated with subsumedBy != null.  Re-anchor pots on the
        // subsumed plane to the plane that absorbed it.
        foreach (var updated in args.updated)
        {
            if (updated.subsumedBy != null)
            {
                ReAnchorPotsToSubsumingPlane(updated);
            }
        }
    }

    /// <summary>
    /// When a plane is subsumed (merged into a larger plane), move any
    /// pots that were on it to the subsuming plane.  Unlike removal,
    /// the subsuming plane is known — we can re-anchor directly to it.
    /// </summary>
    private void ReAnchorPotsToSubsumingPlane(ARPlane oldPlane)
    {
        ARPlane newPlane = oldPlane.subsumedBy;
        if (newPlane == null) return;
        TrackableId oldId = oldPlane.trackableId;

        for (int i = spawned.Count - 1; i >= 0; i--)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || info.anchorObj == null) continue;
            if (info.sourcePlaneId != oldId) continue;

            Vector3    potWorldPos = obj.transform.position;
            Quaternion potWorldRot = obj.transform.rotation;
            Vector3    potLocalScl = obj.transform.localScale;

            Pose reanchorPose = new Pose(potWorldPos, potWorldRot);
            ARAnchor newAnchor = anchorManager.AttachAnchor(newPlane, reanchorPose);
            if (newAnchor == null)
            {
                Debug.LogWarning($"[AR] Subsumption re-anchor failed for pot {i}");
                continue;
            }

            obj.transform.SetParent(newAnchor.transform, true);
            GameObject oldAnchorObj = info.anchorObj;
            info.anchorObj     = newAnchor.gameObject;
            info.sourcePlaneId = newPlane.trackableId;

            info.frozenLocalPos   = obj.transform.localPosition;
            info.frozenLocalRot   = obj.transform.localRotation;
            info.frozenLocalScale = potLocalScl;
            obj.transform.localScale = potLocalScl;

            Destroy(oldAnchorObj);
            Debug.Log($"[AR] Re-anchored pot {i} from subsumed plane {oldId} to {newPlane.trackableId}");
        }
    }

    /// <summary>
    /// When a plane is removed (usually merged into a larger one by ARCore),
    /// find any pots anchored to it and re-anchor them to the best nearby
    /// surviving plane at their current world position.
    /// </summary>
    private void ReAnchorPotsOnRemovedPlane(TrackableId removedPlaneId)
    {
        for (int i = spawned.Count - 1; i >= 0; i--)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || info.anchorObj == null) continue;

            // Only process pots that were actually on the removed plane
            if (info.sourcePlaneId != removedPlaneId) continue;

            var anchor = info.anchorObj.GetComponent<ARAnchor>();
            if (anchor == null) continue;

            // The anchor's transform is still valid for this frame even
            // though its plane was removed. Capture the world position now.
            Vector3 potWorldPos = obj.transform.position;
            Quaternion potWorldRot = obj.transform.rotation;
            Vector3 potLocalScale = obj.transform.localScale;

            // Find the best surviving plane near this pot
            ARPlane bestPlane = FindNearestActivePlane(potWorldPos);
            if (bestPlane == null)
            {
                Debug.LogWarning($"[AR] Plane removed, no replacement found for pot {i}. Pot keeps current anchor.");
                continue;
            }

            // Re-anchor: create new plane-attached anchor at the pot's
            // current world position on the new plane
            Pose reanchorPose = new Pose(potWorldPos, potWorldRot);
            ARAnchor newAnchor = anchorManager.AttachAnchor(bestPlane, reanchorPose);
            if (newAnchor == null)
            {
                Debug.LogWarning($"[AR] Re-anchor failed for pot {i}");
                continue;
            }

            // Re-parent the pot to the new anchor
            obj.transform.SetParent(newAnchor.transform, true);

            // Destroy old anchor
            GameObject oldAnchorObj = info.anchorObj;
            info.anchorObj = newAnchor.gameObject;

            // Re-freeze local transform at new parent
            info.frozenLocalPos   = obj.transform.localPosition;
            info.frozenLocalRot   = obj.transform.localRotation;
            info.frozenLocalScale = potLocalScale;
            obj.transform.localScale = potLocalScale;

            info.sourcePlaneId = bestPlane.trackableId;
            Destroy(oldAnchorObj);
            Debug.Log($"[AR] Re-anchored pot {i} from removed plane {removedPlaneId} to {bestPlane.trackableId}");
        }
    }

    /// <summary>
    /// Find the nearest active, tracked HorizontalUp plane to a world position.
    /// Used when re-anchoring pots after their original plane was removed.
    /// </summary>
    private ARPlane FindNearestActivePlane(Vector3 worldPos)
    {
        if (planeManager == null) return null;

        ARPlane best = null;
        float bestDist = float.MaxValue;

        foreach (var plane in planeManager.trackables)
        {
            if (plane.alignment != PlaneAlignment.HorizontalUp) continue;
            if (plane.trackingState == TrackingState.None) continue;
            if (plane.subsumedBy != null) continue; // Skip merged-away planes

            float dist = Vector3.Distance(worldPos, plane.center);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = plane;
            }
        }
        return best;
    }

    /// <summary>
    /// Sole visibility authority for placed pots.  Checks both the
    /// AR session state AND each anchor's individual tracking state.
    /// Pots are shown only when BOTH are healthy.  This is the ONLY
    /// place in the codebase that calls SetRenderersVisible on pots.
    ///
    /// THROTTLED: runs every HEALTH_CHECK_INTERVAL frames, OR
    /// immediately when the session state transitions.  Avoids
    /// per-frame GetComponentsInChildren and renderer toggling.
    /// </summary>
    private void MonitorAnchorHealth()
    {
        ARSessionState curState = ARSession.state;
        bool stateChanged = curState != _lastSessionState;
        _lastSessionState = curState;

        // Run immediately on state transition; otherwise throttle.
        if (!stateChanged)
        {
            if (++_healthCheckFrame < HEALTH_CHECK_INTERVAL) return;
        }
        _healthCheckFrame = 0;

        bool sessionOK = curState == ARSessionState.SessionTracking;

        for (int i = 0; i < spawned.Count; i++)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || info.anchorObj == null) continue;

            var anchor = info.anchorObj.GetComponent<ARAnchor>();
            if (anchor == null) continue;

            bool visible = sessionOK
                        && anchor.trackingState != TrackingState.None;

            // Only toggle renderers when visibility actually changes
            if (visible != info.lastVisible)
                SetRenderersVisible(info, visible);
        }
    }

    /// <summary>
    /// After the first pot is placed, disable plane mesh visualizers and
    /// hide plane visuals. Plane detection stays ON (so new surfaces can
    /// be found for future placements), but the continuous mesh updates
    /// that cause micro-drift are stopped.
    /// </summary>
    private void DisablePlaneVisualsAfterPlacement()
    {
        if (_planeDetectionReducedAfterPlace) return;
        _planeDetectionReducedAfterPlace = true;

        if (planeManager == null) return;

        // Disable all ARPlaneMeshVisualizer components — these continuously
        // update the plane mesh as ARCore refines boundaries, causing
        // the anchor's world position to shift by 1-10cm.
        foreach (var plane in planeManager.trackables)
        {
            HidePlaneVisual(plane.gameObject);
        }

        // Disable the plane prefab's visualizer so future planes are also hidden
        if (planeManager.planePrefab != null)
            HidePlaneVisual(planeManager.planePrefab);

        Debug.Log("[AR] Plane visuals disabled after first placement");
    }

    private static void HidePlaneVisual(GameObject planeGO)
    {
        // Disable mesh visualizer (stops continuous mesh boundary updates)
        foreach (var vis in planeGO.GetComponents<MonoBehaviour>())
        {
            string tn = vis.GetType().Name;
            if (tn.Contains("PlaneMeshVisualizer") || tn.Contains("PlaneVisualizer"))
            {
                vis.enabled = false;
            }
        }
        // Hide renderers on the plane itself
        foreach (var r in planeGO.GetComponentsInChildren<Renderer>(true))
        {
            if (r) r.enabled = false;
        }
        // Disable MeshRenderer and LineRenderer (plane outline)
        var mr = planeGO.GetComponent<MeshRenderer>();
        if (mr) mr.enabled = false;
        var lr = planeGO.GetComponent<LineRenderer>();
        if (lr) lr.enabled = false;
    }

    // ════════════════════════════════════════════════════════════════
    //  CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    private void ConfigurePlaneDetection()
    {
        if (planeManager != null)
        {
            planeManager.requestedDetectionMode = PlaneDetectionMode.Horizontal;
        }
    }

    /// <summary>
    /// Enable feature-point based detection to help AR subsystem find
    /// surfaces faster.  More feature points = faster plane generation.
    /// </summary>
    private void ConfigureFeaturePoints()
    {
        var pointCloud = FindFirstObjectByType<ARPointCloudManager>();
        if (pointCloud != null)
            pointCloud.enabled = true;
    }

    /// <summary>
    /// Completely suppress the developer console and all debug overlays.
    /// Called in Awake, Start, and enforced every frame via OnGUI.
    /// </summary>
    private void SuppressDeveloperMode()
    {
        Debug.developerConsoleVisible = false;
#if !UNITY_EDITOR
        Debug.unityLogger.filterLogType = LogType.Error;
#endif
    }

    private void DisableXRIInteractors()
    {
        foreach (var mb in FindObjectsByType<MonoBehaviour>(FindObjectsSortMode.None))
        {
            string tn = mb.GetType().Name;
            if (tn.Contains("Interactor") || tn.Contains("XRController")
                || tn.Contains("ScreenSpaceController"))
            {
                mb.enabled = false;
                Debug.Log("[AR] Disabled XRI component: " + tn + " on " + mb.gameObject.name);
            }
        }
    }

    /// <summary>
    /// DISABLE environment occlusion entirely.  On devices like Pixel 4,
    /// the depth estimation is noisy — it causes placed pots to flicker,
    /// partially disappear behind estimated depth planes, or vanish and
    /// reappear.  Turning it OFF means pots render in front of real
    /// surfaces (no hiding behind walls), but they are ALWAYS visible
    /// and solid — which is what the user expects.
    /// </summary>
    private void ConfigureOcclusion()
    {
        if (!arCamera)
        {
            Debug.LogWarning("[AR] ConfigureOcclusion: no camera found");
            return;
        }

        // Disable AROcclusionManager if it exists — do NOT create one
        if (!occlusionManager)
            occlusionManager = arCamera.GetComponent<AROcclusionManager>();
        if (occlusionManager)
        {
            occlusionManager.requestedEnvironmentDepthMode =
                EnvironmentDepthMode.Disabled;
            occlusionManager.requestedOcclusionPreferenceMode =
                OcclusionPreferenceMode.NoOcclusion;
            occlusionManager.enabled = false;
            Debug.Log("[AR] AROcclusionManager DISABLED — pots will always be fully visible");
        }

        // Disable ARShaderOcclusion if present — it also hides virtual objects
        foreach (var c in arCamera.GetComponents<MonoBehaviour>())
        {
            if (c.GetType().Name == "ARShaderOcclusion")
            {
                c.enabled = false;
                Debug.Log("[AR] ARShaderOcclusion DISABLED");
            }
        }

        Debug.Log("[AR] Occlusion fully disabled — pots always render solid");
    }

    private void CleanPreExistingPots()
    {
        if (potPrefabs == null) return;
        var roots = UnityEngine.SceneManagement.SceneManager
                        .GetActiveScene().GetRootGameObjects();
        foreach (var root in roots)
        {
            if (root == gameObject) continue;
            if (spawned.Contains(root)) continue;
            foreach (var prefab in potPrefabs)
            {
                if (!prefab) continue;
                if (root.name == prefab.name
                    && root.GetComponentInChildren<MeshRenderer>())
                {
                    Debug.Log("[AR] Removing stale scene object: " + root.name);
                    Destroy(root);
                    break;
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  TOUCH INPUT
    // ════════════════════════════════════════════════════════════════
    private void HandleTouch()
    {
        if (menuGO && menuGO.activeSelf) return;

        Vector2 pos = Vector2.zero;
        bool tap = false;

        var touches = ETouch.activeTouches;
        if (touches.Count == 1
            && touches[0].phase == TouchPhaseIS.Began)
        {
            pos = touches[0].screenPosition;
            tap = true;
        }

#if UNITY_EDITOR
        if (!tap && Mouse.current != null
            && Mouse.current.leftButton.wasPressedThisFrame)
        {
            pos = Mouse.current.position.ReadValue();
            tap = true;
        }
#endif

        if (!tap) return;
        if (IsOverUI(pos)) return;
        if (Time.unscaledTime - lastTapTime < TAP_CD) return;
        lastTapTime = Time.unscaledTime;

        // Check if tapped an existing pot
        Ray ray = arCamera.ScreenPointToRay(pos);
        if (Physics.Raycast(ray, out RaycastHit hit, 100f))
        {
            GameObject tapped = FindSpawnedRoot(hit.collider.transform);
            if (tapped != null)
            {
                if (deleteMode) { DeleteObject(tapped); return; }
                ShowLabelForPot(tapped);
                return;
            }
        }

        if (deleteMode) return;

        // Auto-close any open measurement label when tapping empty space
        HideAllLabels();

        // ── TWO-PASS PLANE RAYCAST ──
        // Pass 1: PlaneWithinPolygon ONLY — this uses the real detected polygon
        //   boundary.  It will NOT match the extended rectangular bounding box
        //   of a table, so rays aimed at the floor behind a table pass through
        //   the table and correctly hit the floor polygon.
        // Pass 2 (fallback): PlaneWithinBounds — for newly detected planes that
        //   don't have a refined polygon yet.  Only used when Pass 1 finds nothing.
        ARPlane  bestPlane = null;
        ARRaycastHit bestHit = default;

        // --- PASS 1: tight polygon ---
        if (raycastManager.Raycast(pos, sHits, PlaneFilterTight))
        {
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);
        }

        // --- PASS 2: loose bounds (fallback) ---
        // Bounds can extend past the real polygon edge, so require
        // fully-Tracking state (not Limited) to reduce false hits
        // on unstable or edge-case surfaces.
        if (bestPlane == null && raycastManager.Raycast(pos, sHits, PlaneFilterLoose))
        {
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);
            if (bestPlane != null && bestPlane.trackingState != TrackingState.Tracking)
                bestPlane = null;
        }

        if (bestPlane != null)
        {
            PlaceObject(bestHit.pose, bestPlane);
            return;
        }

        ShowToast("Slowly aim at a flat surface like floor or table");
    }

    /// <summary>
    /// From a list of AR raycast hits, find the best valid HorizontalUp
    /// plane.  Always prefer the CLOSEST hit (nearest to camera along
    /// the ray).  The user taps on what they SEE — if they tap a table,
    /// the table plane is the closest hit; the floor behind/under it is
    /// farther. PlaneWithinPolygon already prevents false hits from
    /// extended bounding boxes, so closest == user intent.
    /// </summary>
    private void PickBestPlaneHit(List<ARRaycastHit> hits,
        ref ARPlane bestPlane, ref ARRaycastHit bestHit)
    {
        float bestDist = float.MaxValue;

        for (int i = 0; i < hits.Count; i++)
        {
            var arHit = hits[i];
            ARPlane plane = arHit.trackable as ARPlane;
            if (!IsValidPlaneHit(arHit, plane)) continue;

            // Simply pick the closest valid plane — that's what the user tapped on
            if (arHit.distance < bestDist)
            {
                bestDist  = arHit.distance;
                bestHit   = arHit;
                bestPlane = plane;
            }
        }
    }

    /// <summary>Validate that an AR hit + plane passes all placement gates.</summary>
    private bool IsValidPlaneHit(ARRaycastHit arHit, ARPlane plane)
    {
        if (plane == null) return false;
        float angle = Vector3.Angle(arHit.pose.up, Vector3.up);
        if (angle >= MAX_SURFACE_ANGLE_DEG) return false;
        if (plane.alignment != PlaneAlignment.HorizontalUp) return false;
        if (plane.trackingState == TrackingState.None) return false;
        if (plane.size.x * plane.size.y < MIN_PLANE_AREA_SQM) return false;
        return true;
    }

    private bool IsOverUI(Vector2 screenPos)
    {
        if (EventSystem.current == null) return false;
        var ped = new PointerEventData(EventSystem.current) { position = screenPos };
        uiHits.Clear();
        EventSystem.current.RaycastAll(ped, uiHits);
        for (int i = 0; i < uiHits.Count; i++)
        {
            var go = uiHits[i].gameObject;
            if (go.layer == 5) return true;
            if (go.GetComponentInParent<Button>()) return true;
        }
        return false;
    }

    private GameObject FindSpawnedRoot(Transform t)
    {
        while (t != null)
        {
            if (spawned.Contains(t.gameObject)) return t.gameObject;
            t = t.parent;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  PLACEMENT
    // ════════════════════════════════════════════════════════════════
    private void PlaceObject(Pose pose, ARPlane plane)
    {
        if (potPrefabs == null || potPrefabs.Length == 0) return;
        int idx = Mathf.Clamp(selectedPotIndex, 0, potPrefabs.Length - 1);
        GameObject prefab = potPrefabs[idx];
        if (!prefab) return;

        // GATE: AR must be fully tracking
        if (ARSession.state != ARSessionState.SessionTracking)
        { ShowToast("Waiting for AR tracking…"); return; }

        // GATE: require a HorizontalUp plane
        if (plane == null)
        { ShowToast("No detected surface – aim at the floor"); return; }
        if (plane.alignment != PlaneAlignment.HorizontalUp)
        { ShowToast("Horizontal surface required"); return; }
        if (plane.trackingState == TrackingState.None)
        { ShowToast("Surface lost – move phone slowly"); return; }

        // GATE: overlap + stacking check
        if (spawned.Count > 0)
        {
            Bounds candidate = EstimatePlacedBounds(pose, idx);
            if (WouldOverlapOrStack(candidate))
            { ShowToast("Too close to another pot!"); return; }
        }

        SpawnNewPot(pose, plane, idx, prefab);
    }

    private void SpawnNewPot(Pose pose, ARPlane plane, int idx, GameObject prefab)
    {
        if (!TryAnchor(pose, plane, out ARAnchor anchor))
            return;

        GameObject obj = Instantiate(prefab, anchor.transform);
        obj.transform.localPosition = Vector3.zero;
        obj.transform.localRotation = Quaternion.identity;

        // Isolate on dedicated physics layer — zero interaction with anything
        SetLayerRecursive(obj, POT_PHYSICS_LAYER);

        Debug.Log($"[AR] SpawnNewPot: anchor={anchor.gameObject.name}"
            + $", worldPos={anchor.transform.position}"
            + $", planeId={plane.trackableId}"
            + $", planeTracking={plane.trackingState}");

        // Uniform default size: ALL pots start at DEFAULT_POT_HEIGHT_CM
        // regardless of prefab index.  User can pinch-resize afterwards.
        Vector3 ns = NormalizeHeight(obj, DEFAULT_POT_HEIGHT_CM);
        obj.transform.localScale = ns * Mathf.Max(defaultScale, 0.1f);

        // Face camera at placement time — use LOCAL rotation so anchor
        // tilt doesn't fight the freeze.  Compute desired world rotation,
        // then convert to local space relative to anchor.
        if (arCamera)
        {
            Vector3 dir = arCamera.transform.position - pose.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.001f)
            {
                Quaternion worldRot = Quaternion.LookRotation(-dir.normalized)
                    * Quaternion.Euler(prefabRotationOffsetEuler);
                // Convert to local: localRot = inverse(parentWorldRot) * worldRot
                obj.transform.localRotation =
                    Quaternion.Inverse(anchor.transform.rotation) * worldRot;
            }
        }

        FitCollider(obj);
        GroundChild(obj);
        StripPhysicsAndScripts(obj);

        // Force all materials to fully opaque (no transparency artifacts)
        ForceOpaqueMaterials(obj);

        // Prevent AR occlusion from ever hiding this pot's renderers.
        // Even if someone re-enables occlusion, these renderers will
        // always draw in front of the depth buffer.
        ForceRendererAlwaysVisible(obj);

        // Make the fitted collider a trigger — zero physics resolution
        var topCol = obj.GetComponent<BoxCollider>();
        if (topCol) topCol.isTrigger = true;

        var info        = obj.AddComponent<PotInfo>();
        info.idx        = idx;
        info.surfaceY   = pose.position.y + yOffset;
        info.normScale  = ns;
        info.anchorObj  = anchor.gameObject;
        info.sourcePlaneId = plane.trackableId;
        info.measureLabel = CreateMeasureLabel(obj, idx);
        // Cache renderers once — avoids GetComponentsInChildren every frame
        info.cachedRenderers = obj.GetComponentsInChildren<Renderer>(true);

        // FREEZE the local transform — from this point, LateUpdate
        // enforces these values every frame.  The pot's world position
        // is controlled entirely by the ARAnchor parent via SLAM.
        info.frozenLocalPos   = obj.transform.localPosition;
        info.frozenLocalRot   = obj.transform.localRotation;
        info.frozenLocalScale = obj.transform.localScale;
        info.isFrozen         = true;

        // Validate parent chain: pot must NOT be under camera.
        // If bad, destroy everything and abort — do not salvage.
        if (!ValidateParentChain(obj, anchor))
        {
            if (info.measureLabel) Destroy(info.measureLabel);
            Destroy(anchor.gameObject); // destroys anchor + pot (child)
            ShowToast("Placement failed — try another spot");
            return;
        }

        // Sync physics one final time so collider is at correct world pos
        Physics.SyncTransforms();

        spawned.Add(obj);
        ShowLabelForPot(obj);

        // After first placement, disable plane mesh visualizers to stop
        // boundary refinement from micro-drifting anchors.
        DisablePlaneVisualsAfterPlacement();

        StartCoroutine(VerifyPlacement(obj, obj.transform.position));
        ShowToast("Pot placed!");
    }

    private IEnumerator VerifyPlacement(GameObject pot, Vector3 expectedWorldPos)
    {
        yield return new WaitForSeconds(2f);
        if (pot == null) yield break;
        float drift = Vector3.Distance(pot.transform.position, expectedWorldPos);
        var info = pot.GetComponent<PotInfo>();
        Debug.Log($"[AR] PlacementVerify: drift={drift:F4}m, pos={pot.transform.position}"
            + $", expected={expectedWorldPos}"
            + $", anchor={info?.anchorObj?.name}");
        // Small drift (<10cm) is normal — ARCore refines plane positions.
        // The anchor system handles this; only log excessive drift.
        if (drift > 0.10f)
            Debug.LogWarning("[AR] Excessive anchor drift: " + drift.ToString("F3") + "m in 2s");
    }

    // ════════════════════════════════════════════════════════════════
    //  OVERLAP & ANTI-STACKING
    // ════════════════════════════════════════════════════════════════

    private bool WouldOverlapOrStack(Bounds candidate, GameObject exclude = null)
    {
        if (spawned.Count == 0) return false;

        Vector2 cXZ = new Vector2(candidate.center.x, candidate.center.z);
        float   cR  = Mathf.Max(candidate.extents.x, candidate.extents.z);

        for (int i = 0; i < spawned.Count; i++)
        {
            var existing = spawned[i];
            if (!existing || existing == exclude) continue;
            Bounds eb = WorldBounds(existing);
            if (eb.size == Vector3.zero) continue;

            // ── Y-level separation: skip pots on different surfaces ──
            bool yOverlaps = candidate.min.y < eb.max.y
                          && candidate.max.y > eb.min.y;
            if (!yOverlaps) continue;

            // ── Cylindrical XZ overlap (fairer for round pots) ──
            Vector2 eXZ = new Vector2(eb.center.x, eb.center.z);
            float   eR  = Mathf.Max(eb.extents.x, eb.extents.z);
            float   xzDist = Vector2.Distance(cXZ, eXZ);
            float   minSep = cR + eR + MIN_GAP_METRES;

            // ── ANTI-STACKING: block pot placed ON TOP of another ──
            float cBottom = candidate.min.y;
            float eTop    = eb.max.y;
            if (cBottom >= eTop - STACK_Y_BLOCK && xzDist < minSep)
                return true;

            // ── Same surface: real horizontal collision ──
            if (xzDist < minSep)
                return true;
        }
        return false;
    }

    private Bounds EstimatePlacedBounds(Pose pose, int idx)
    {
        float rh = GetRealHeight(idx);
        GameObject prefab = potPrefabs[Mathf.Clamp(idx, 0, potPrefabs.Length - 1)];
        if (!prefab) return new Bounds(pose.position, Vector3.zero);

        Bounds lb = LocalBounds(prefab);
        if (lb.size == Vector3.zero)
            return new Bounds(pose.position, Vector3.zero);

        float normF = (rh / 100f) / Mathf.Max(0.0001f, lb.size.y);
        float scaleFactor = Mathf.Max(defaultScale, 0.1f) * normF;

        // Use exact real size — no inflation. MIN_GAP_METRES handles separation.
        Vector3 size = lb.size * scaleFactor;
        Vector3 center = pose.position + new Vector3(0, size.y * 0.5f, 0);
        return new Bounds(center, size);
    }

    // ════════════════════════════════════════════════════════════════
    //  ANCHOR  —  PLANE-ATTACHED (proper SLAM tracking) + Y-LOCK
    // ════════════════════════════════════════════════════════════════
    /// <summary>
    /// Create a PLANE-ATTACHED anchor for proper ARCore SLAM tracking.
    /// The anchor is registered with the AR subsystem and participates
    /// in the device’s global map — it will NOT walk with the camera.
    ///
    /// The Y-axis drift (table→floor sliding) that plane-attached
    /// anchors can exhibit is handled separately by the Y-LOCK guard
    /// in EnforceFrozenTransforms.
    /// </summary>
    private bool TryAnchor(Pose pose, ARPlane plane, out ARAnchor anchor)
    {
        anchor = null;
        if (anchorManager == null)
        {
            Debug.LogError("[AR] TryAnchor: anchorManager is NULL");
            ShowToast("Anchor system unavailable");
            return false;
        }

        if (plane == null)
        { ShowToast("No plane – aim at a flat surface"); return false; }

        if (plane.trackingState == TrackingState.None)
        { ShowToast("Surface tracking lost – move slowly"); return false; }

        // Plane-attached anchor: ARCore tracks this in the world SLAM map.
        // The anchor stays in the physical world, NOT relative to camera.
        anchor = anchorManager.AttachAnchor(plane, pose);
        if (anchor == null)
        { ShowToast("Anchor failed – try another spot"); return false; }

        Debug.Log($"[AR] Plane-attached anchor: {anchor.gameObject.name}"
            + $", worldPos={pose.position}"
            + $", planeId={plane.trackableId}"
            + $", tracking={anchor.trackingState}");
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  GROUNDING & BOUNDS
    // ════════════════════════════════════════════════════════════════
    /// <summary>
    /// Ground the pot so its visual bottom sits exactly on the anchor's
    /// Y plane.  Uses LOCAL-space bounds to avoid world-space rounding
    /// errors that cause gaps or sinking.  The anchor's own Y IS the
    /// surface — we only need to shift the child so its mesh bottom
    /// aligns with localPosition.y = 0 in anchor space.
    /// </summary>
    private void GroundChild(GameObject obj)
    {
        Physics.SyncTransforms();

        // Get bounds in the pot's own local space (rotation-independent)
        Bounds lb = LocalBounds(obj);
        if (lb.size == Vector3.zero) return;

        // lb.min.y is the lowest point in the pot's local coords.
        // We want that point to land at the anchor's Y + yOffset.
        // Since the pot is a direct child of the anchor:
        //   worldBottom = anchor.pos.y + localPos.y + lb.min.y * scaleY
        // We want worldBottom = anchor.pos.y + yOffset
        //   => localPos.y = yOffset - lb.min.y * scaleY
        float scaleY = obj.transform.localScale.y;
        Vector3 lp = obj.transform.localPosition;
        lp.y = yOffset - lb.min.y * scaleY;
        obj.transform.localPosition = lp;

        Physics.SyncTransforms();

        // DOUBLE-PASS: re-read world bounds and verify pot bottom is
        // exactly at surface.  Corrects floating-point accumulation
        // from rotation + non-uniform scale.
        Bounds wb = WorldBounds(obj);
        if (wb.size != Vector3.zero && obj.transform.parent != null)
        {
            float surfaceY = obj.transform.parent.position.y + yOffset;
            float gap = wb.min.y - surfaceY;
            if (Mathf.Abs(gap) > 0.001f)
            {
                lp = obj.transform.localPosition;
                lp.y -= gap;
                obj.transform.localPosition = lp;
                Physics.SyncTransforms();
            }
        }
    }

    private Bounds WorldBounds(GameObject root)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Renderer first = null;
        foreach (var r in rs)
            if (!IgnoreRenderer(r)) { first = r; break; }
        if (!first) return new Bounds(root.transform.position, Vector3.zero);
        Bounds b = first.bounds;
        foreach (var r in rs)
            if (!IgnoreRenderer(r)) b.Encapsulate(r.bounds);
        return b;
    }

    private Bounds LocalBounds(GameObject root)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Matrix4x4 w2l = root.transform.worldToLocalMatrix;
        Bounds lb = new Bounds(Vector3.zero, Vector3.zero);
        bool first = true;
        foreach (var r in rs)
        {
            if (IgnoreRenderer(r)) continue;
            Bounds wb = r.bounds;
            Vector3 mn = wb.min, mx = wb.max;
            for (int i = 0; i < 8; i++)
            {
                Vector3 corner = new Vector3(
                    (i & 1) == 0 ? mn.x : mx.x,
                    (i & 2) == 0 ? mn.y : mx.y,
                    (i & 4) == 0 ? mn.z : mx.z);
                Vector3 lp = w2l.MultiplyPoint3x4(corner);
                if (first) { lb = new Bounds(lp, Vector3.zero); first = false; }
                else lb.Encapsulate(lp);
            }
        }
        return lb;
    }

    private bool IgnoreRenderer(Renderer r)
    {
        if (!r) return true;
        if (ignoreRendererNameContains == null) return false;
        string n = r.gameObject.name.ToLowerInvariant();
        foreach (var s in ignoreRendererNameContains)
            if (!string.IsNullOrEmpty(s) && n.Contains(s.ToLowerInvariant()))
                return true;
        return false;
    }

    private void FitCollider(GameObject root)
    {
        Bounds w = WorldBounds(root);
        if (w.size == Vector3.zero) return;
        BoxCollider bc = root.GetComponent<BoxCollider>()
                         ?? root.AddComponent<BoxCollider>();
        bc.center = root.transform.InverseTransformPoint(w.center);
        Vector3 ls = root.transform.lossyScale;
        bc.size = new Vector3(
            ls.x != 0 ? w.size.x / ls.x : w.size.x,
            ls.y != 0 ? w.size.y / ls.y : w.size.y,
            ls.z != 0 ? w.size.z / ls.z : w.size.z);
    }

    private Vector3 NormalizeHeight(GameObject root, float cm)
    {
        Bounds b = WorldBounds(root);
        float f = (cm / 100f) / Mathf.Max(0.0001f, b.size.y);
        root.transform.localScale = Vector3.one * f;
        return root.transform.localScale;
    }

    private float GetRealHeight(int i)
    {
        if (potRealHeightCm == null || i < 0 || i >= potRealHeightCm.Length)
            return DEFAULT_POT_HEIGHT_CM;
        float h = potRealHeightCm[i];
        return h > 0.01f ? h : DEFAULT_POT_HEIGHT_CM;
    }

    /// <summary>
    /// Validate that the pot is NOT accidentally parented under the
    /// camera or camera offset, which would cause it to follow the user.
    /// Returns true if the chain is safe.  Returns false if the chain
    /// is bad — the caller must destroy the anchor+pot and abort.
    /// We do NOT attempt to salvage a bad chain (deparenting a
    /// mis-parented anchor leaves it at a meaningless world pose).
    /// </summary>
    private bool ValidateParentChain(GameObject pot, ARAnchor anchor)
    {
        Transform t = pot.transform.parent;
        while (t != null)
        {
            if (t == arCamera?.transform)
            {
                Debug.LogError("[AR] CRITICAL: Pot parented under camera — aborting placement.");
                return false;
            }
            if (t.name.Contains("Camera Offset") || t.name.Contains("CameraOffset"))
            {
                Debug.LogError("[AR] CRITICAL: Pot under Camera Offset — aborting placement.");
                return false;
            }
            t = t.parent;
        }
        return true;
    }

    /// <summary>Set layer on object and all children recursively.</summary>
    private static void SetLayerRecursive(GameObject obj, int layer)
    {
        obj.layer = layer;
        foreach (Transform child in obj.transform)
            SetLayerRecursive(child.gameObject, layer);
    }

    /// <summary>
    /// Force every renderer on the pot to always be visible — never
    /// culled by the camera frustum or hidden by AR occlusion.
    /// This prevents the "disappearing pot" effect when the depth
    /// buffer erroneously thinks a pot is behind a real surface.
    /// </summary>
    private static void ForceRendererAlwaysVisible(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (!r) continue;
            // Disable dynamic occlusion (AR depth-based culling)
            r.allowOcclusionWhenDynamic = false;
            // NOTE: r.bounds is a struct (value-type copy) — calling
            // Expand on it has no effect.  We rely on
            // allowOcclusionWhenDynamic=false + disabled AROcclusionManager
            // to keep pots visible at all times.
        }
    }

    /// <summary>
    /// Force all renderers on the pot to fully opaque rendering.
    /// Prevents any accidental transparency that makes pots look ghostly.
    /// </summary>
    private static void ForceOpaqueMaterials(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (!r) continue;
            foreach (var mat in r.materials)
            {
                if (!mat) continue;
                // URP Lit: _Surface 0 = Opaque, 1 = Transparent
                if (mat.HasProperty("_Surface"))
                {
                    mat.SetFloat("_Surface", 0f); // Opaque
                    mat.SetFloat("_Blend", 0f);   // Alpha blend off
                }
                // Standard shader
                if (mat.HasProperty("_Mode"))
                    mat.SetFloat("_Mode", 0f); // Opaque
                // Force full alpha on base color
                if (mat.HasProperty("_BaseColor"))
                {
                    Color c = mat.GetColor("_BaseColor");
                    c.a = 1f;
                    mat.SetColor("_BaseColor", c);
                }
                if (mat.HasProperty("_Color"))
                {
                    Color c = mat.GetColor("_Color");
                    c.a = 1f;
                    mat.SetColor("_Color", c);
                }
                // Disable transparency keywords
                mat.DisableKeyword("_ALPHAPREMULTIPLY_ON");
                mat.DisableKeyword("_ALPHATEST_ON");
                mat.DisableKeyword("_ALPHABLEND_ON");
                mat.DisableKeyword("_SURFACE_TYPE_TRANSPARENT");
                // Set render queue to geometry (opaque)
                mat.renderQueue = 2000;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  STRIP PHYSICS & SCRIPTS  (nothing can move the pot)
    // ════════════════════════════════════════════════════════════════
    private static void StripPhysicsAndScripts(GameObject root)
    {
        // ── NUCLEAR: destroy EVERYTHING that could cause movement ──
        foreach (var j in root.GetComponentsInChildren<Joint>(true))
            Destroy(j);
        foreach (var rb in root.GetComponentsInChildren<Rigidbody>(true))
        {
            rb.isKinematic = true;
            rb.detectCollisions = false;
            Destroy(rb);
        }
        foreach (var cl in root.GetComponentsInChildren<Cloth>(true))
            Destroy(cl);
        foreach (var a in root.GetComponentsInChildren<Animator>(true))
            Destroy(a);
        foreach (var a in root.GetComponentsInChildren<Animation>(true))
            Destroy(a);
        // Destroy CharacterControllers and NavMeshAgents (if any)
        foreach (var cc in root.GetComponentsInChildren<CharacterController>(true))
            Destroy(cc);
        foreach (var mb in root.GetComponentsInChildren<MonoBehaviour>(true))
        {
            if (mb is PotInfo) continue;
            string typeName = mb.GetType().Name;
            if (typeName.Contains("Interactable") || typeName.Contains("Interactor")
                || typeName.Contains("ARTranslation") || typeName.Contains("ARRotation")
                || typeName.Contains("ARScale") || typeName.Contains("ARPlacement")
                || typeName.Contains("Transformer") || typeName.Contains("Manipulator")
                || typeName.Contains("NavMeshAgent") || typeName.Contains("FollowTarget")
                || typeName.Contains("LookAt") || typeName.Contains("Constraint"))
            {
                Destroy(mb);
                continue;
            }
            mb.enabled = false;
        }
        // Destroy all child colliders (we keep only the top-level fitted BoxCollider)
        // Child colliders can participate in physics resolution and cause micro-jitter
        var allCols = root.GetComponentsInChildren<Collider>(true);
        foreach (var col in allCols)
        {
            if (col.gameObject == root) continue; // keep top-level
            Destroy(col);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MEASUREMENT LABELS (world-space, anchor-parented)
    // ════════════════════════════════════════════════════════════════
    private void ShowLabelForPot(GameObject pot)
    {
        activeLabelPot = pot;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi == null || !pi.measureLabel) continue;
            pi.measureLabel.SetActive(obj == pot);
        }
        if (pot != null)
        {
            var pi = pot.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel)
                pi.measureLabel.SetActive(true);
        }
    }

    /// <summary>Hide ALL measurement labels (called on empty-space tap).</summary>
    private void HideAllLabels()
    {
        activeLabelPot = null;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel)
                pi.measureLabel.SetActive(false);
        }
    }

    private GameObject CreateMeasureLabel(GameObject pot, int idx)
    {
        Bounds wb = WorldBounds(pot);
        if (wb.size == Vector3.zero) return null;

        // Use local bounds × lossyScale for EXACT physical dimensions.
        // WorldBounds AABB overestimates W/D for rotated non-symmetric
        // pots because the axis-aligned box inflates diagonally.
        // Height (Y) is always accurate since rotation is Y-axis only.
        Bounds lb = LocalBounds(pot);
        Vector3 ls = pot.transform.lossyScale;
        float hCm = lb.size.y * Mathf.Abs(ls.y) * 100f;
        float actualW = lb.size.x * Mathf.Abs(ls.x);
        float actualD = lb.size.z * Mathf.Abs(ls.z);
        float wCm = Mathf.Max(actualW, actualD) * 100f;
        float dCm = Mathf.Min(actualW, actualD) * 100f;

        var labelGO = new GameObject("MeasureLabel");
        Transform labelParent = pot.transform.parent ? pot.transform.parent : pot.transform;
        labelGO.transform.SetParent(labelParent, true);

        var cv = labelGO.AddComponent<Canvas>();
        cv.renderMode   = RenderMode.WorldSpace;
        cv.sortingOrder = 200;
        labelGO.AddComponent<CanvasScaler>().dynamicPixelsPerUnit = 100;

        var cvRT = labelGO.GetComponent<RectTransform>();
        cvRT.sizeDelta = new Vector2(240, 150);
        float s = 0.0008f;
        cvRT.localScale = new Vector3(s, s, s);

        labelGO.transform.position =
            wb.center + new Vector3(0, wb.extents.y + 0.08f, 0);

        // White card
        var card = new GameObject("Card");
        card.transform.SetParent(cvRT, false);
        var crt = card.AddComponent<RectTransform>();
        crt.anchorMin = new Vector2(0, 0.22f);
        crt.anchorMax = Vector2.one;
        crt.offsetMin = crt.offsetMax = Vector2.zero;
        var cardImg = card.AddComponent<Image>();
        cardImg.color = COL_WHITE;
        cardImg.raycastTarget = false;

        // Height text
        var hGO = new GameObject("HTxt");
        hGO.transform.SetParent(card.transform, false);
        var htmp = hGO.AddComponent<TextMeshProUGUI>();
        htmp.text      = string.Format("H: {0:F1} cm", hCm);
        htmp.fontSize  = 34;
        htmp.color     = COL_MGREY;
        htmp.alignment = TextAlignmentOptions.Center;
        htmp.raycastTarget = false;
        var hrt = hGO.GetComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 0.5f);
        hrt.anchorMax = Vector2.one;
        hrt.offsetMin = hrt.offsetMax = Vector2.zero;

        // Width × Depth text
        var wGO = new GameObject("WDTxt");
        wGO.transform.SetParent(card.transform, false);
        var wtmp = wGO.AddComponent<TextMeshProUGUI>();
        wtmp.text      = string.Format("W: {0:F1}  D: {1:F1} cm", wCm, dCm);
        wtmp.fontSize  = 30;
        wtmp.color     = COL_MGREY;
        wtmp.alignment = TextAlignmentOptions.Center;
        wtmp.raycastTarget = false;
        var wrt = wGO.GetComponent<RectTransform>();
        wrt.anchorMin = Vector2.zero;
        wrt.anchorMax = new Vector2(1, 0.5f);
        wrt.offsetMin = wrt.offsetMax = Vector2.zero;

        // Arrow stem
        var stem = new GameObject("Stem");
        stem.transform.SetParent(cvRT, false);
        var srt = stem.AddComponent<RectTransform>();
        srt.anchorMin = srt.anchorMax = new Vector2(0.5f, 0);
        srt.pivot = new Vector2(0.5f, 1);
        srt.anchoredPosition = new Vector2(0, cvRT.sizeDelta.y * 0.22f);
        srt.sizeDelta = new Vector2(4, 28);
        var stemImg = stem.AddComponent<Image>();
        stemImg.color = COL_MGREY;
        stemImg.raycastTarget = false;

        // Arrowhead
        var head = new GameObject("Head");
        head.transform.SetParent(cvRT, false);
        var hdrt = head.AddComponent<RectTransform>();
        hdrt.anchorMin = hdrt.anchorMax = new Vector2(0.5f, 0);
        hdrt.pivot = new Vector2(0.5f, 0.5f);
        hdrt.anchoredPosition = new Vector2(0, -6);
        hdrt.sizeDelta = new Vector2(14, 14);
        hdrt.localRotation = Quaternion.Euler(0, 0, 45);
        var headImg = head.AddComponent<Image>();
        headImg.color = COL_MGREY;
        headImg.raycastTarget = false;

        // Standard depth testing
        foreach (var img in labelGO.GetComponentsInChildren<Image>(true))
        {
            Material m = new Material(img.material ?? Canvas.GetDefaultCanvasMaterial());
            m.SetInt(ZTestMode, ZTEST_LEQUAL);
            img.material = m;
        }
        foreach (var tmp in labelGO.GetComponentsInChildren<TextMeshProUGUI>(true))
        {
            Material m = new Material(tmp.fontSharedMaterial);
            m.SetInt(ZTestMode, ZTEST_LEQUAL);
            tmp.fontSharedMaterial = m;
        }

        return labelGO;
    }

    private void BillboardLabels()
    {
        // Early-out: skip iteration when no label is shown
        if (!arCamera || activeLabelPot == null) return;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || !info.measureLabel
                || !info.measureLabel.activeSelf) continue;
            var t = info.measureLabel.transform;
            Vector3 dir = t.position - arCamera.transform.position;
            if (dir.sqrMagnitude > 0.001f)
                t.rotation = Quaternion.LookRotation(dir);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PINCH SCALE
    // ════════════════════════════════════════════════════════════════
    private void HandlePinch()
    {
        var touches = ETouch.activeTouches;
        if (touches.Count != 2 || deleteMode) return;

        var t0 = touches[0];
        var t1 = touches[1];
        float dist = Vector2.Distance(t0.screenPosition, t1.screenPosition);

        if (t0.phase == TouchPhaseIS.Began
            || t1.phase == TouchPhaseIS.Began)
        {
            Vector2 mid = (t0.screenPosition + t1.screenPosition) * 0.5f;
            Ray ray = arCamera.ScreenPointToRay(mid);
            pinchTarget = null;
            Physics.SyncTransforms();
            if (Physics.Raycast(ray, out RaycastHit hit, 100f))
                pinchTarget = FindSpawnedRoot(hit.collider.transform);
            if (pinchTarget == null && spawned.Count > 0)
                pinchTarget = spawned[spawned.Count - 1];
            if (pinchTarget == null) return;

            pinchStartDist  = dist;
            pinchStartScale = pinchTarget.transform.localScale;
            return;
        }

        if (pinchTarget == null || pinchStartDist < 1f) return;

        var info = pinchTarget.GetComponent<PotInfo>();

        // Unfreeze BEFORE any transform writes so LateUpdate
        // doesn't slam old values back between HandlePinch frames.
        if (info != null) info.isFrozen = false;

        float ratio = dist / pinchStartDist;
        Vector3 bs = info != null ? info.normScale : Vector3.one;
        float curFactor = pinchStartScale.x / Mathf.Max(bs.x, 0.0001f);
        float tgtFactor = Mathf.Clamp(curFactor * ratio, minScale, maxScale);
        pinchTarget.transform.localScale = bs * tgtFactor;

        GroundChild(pinchTarget);

        // Block scaling ONLY if it causes actual physical overlap/stacking.
        // Use real world bounds — not inflated estimates.
        if (tgtFactor > curFactor && spawned.Count > 1)
        {
            Bounds scaled = WorldBounds(pinchTarget);
            if (scaled.size != Vector3.zero
                && WouldOverlapOrStack(scaled, pinchTarget))
            {
                pinchTarget.transform.localScale = pinchStartScale;
                GroundChild(pinchTarget);
                ShowToast("Can't scale — touching another pot");
            }
        }

        // Re-freeze IMMEDIATELY with updated local transform and rebuild label
        if (info != null)
        {
            info.frozenLocalPos   = pinchTarget.transform.localPosition;
            info.frozenLocalRot   = pinchTarget.transform.localRotation;
            info.frozenLocalScale = pinchTarget.transform.localScale;
            info.isFrozen         = true;   // Re-freeze on SAME frame — no gap

            if (info.measureLabel) Destroy(info.measureLabel);
            info.measureLabel = CreateMeasureLabel(pinchTarget, info.idx);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE MODE  (NO transparency / tinting — pots stay 100% solid)
    // ════════════════════════════════════════════════════════════════
    private void SetDeleteMode(bool on)
    {
        deleteMode = on;
        if (bannerGO) bannerGO.SetActive(on);
        // NO material changes — pots NEVER become transparent.
        // The red banner is the only visual indicator of delete mode.
    }

    private void DeleteObject(GameObject obj)
    {
        if (!obj) return;
        spawned.Remove(obj);
        var info = obj.GetComponent<PotInfo>();
        if (info != null)
        {
            if (info.measureLabel) Destroy(info.measureLabel);
            // Destroying the anchor also destroys the pot (it's a child).
            // If anchorObj is missing, destroy the pot directly.
            GameObject toDestroy = info.anchorObj ? info.anchorObj : obj;
            Destroy(toDestroy);
        }
        else
            Destroy(obj);
        _spawnedDirty = true;
        SetDeleteMode(false);
        ShowToast("Pot removed");
    }

    // ════════════════════════════════════════════════════════════════
    //  SCREENSHOT
    // ════════════════════════════════════════════════════════════════
    private void TakeScreenshot() => StartCoroutine(CaptureCoroutine());

    private IEnumerator CaptureCoroutine()
    {
        // ── Snapshot current UI visibility ──
        bool barOn    = barGO && barGO.activeSelf;
        bool menuOn   = menuGO && menuGO.activeSelf;
        bool banOn    = bannerGO && bannerGO.activeSelf;
        bool toastOn  = toastGO && toastGO.activeSelf;

        // ── HIDE ALL UI for clean capture ──
        if (barGO)    barGO.SetActive(false);
        if (menuGO)   menuGO.SetActive(false);
        if (bannerGO) bannerGO.SetActive(false);
        if (toastGO)  toastGO.SetActive(false);

        // Hide world-space measurement labels so they don't appear in screenshot
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel)
                pi.measureLabel.SetActive(false);
        }

        yield return new WaitForEndOfFrame();

        Texture2D tex = ScreenCapture.CaptureScreenshotAsTexture();
        if (tex == null)
        {
            RestoreUI(barOn, menuOn, banOn, toastOn);
            if (activeLabelPot != null) ShowLabelForPot(activeLabelPot);
            ShowToast("Capture failed");
            yield break;
        }

        byte[] png = tex.EncodeToPNG();
        Destroy(tex);

        string designName = "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");
        string fn = designName + ".png";
        string savedPath = null;

#if UNITY_ANDROID && !UNITY_EDITOR
        string folder = "/storage/emulated/0/DCIM/MyDesigns/";
        try
        {
            Directory.CreateDirectory(folder);
            savedPath = Path.Combine(folder, fn);
            File.WriteAllBytes(savedPath, png);
            using (var scanClass = new AndroidJavaClass("android.media.MediaScannerConnection"))
            using (var player    = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (var ctx       = player.GetStatic<AndroidJavaObject>("currentActivity"))
                scanClass.CallStatic("scanFile", ctx, new string[] { savedPath }, null, null);
            onScreenshotSaved?.Invoke(savedPath);
        }
        catch (Exception e) { Debug.LogError("[AR] Screenshot save: " + e.Message); }
#else
        string dir = Path.Combine(Application.persistentDataPath, screenshotFolderName);
        Directory.CreateDirectory(dir);
        savedPath = Path.Combine(dir, fn);
        File.WriteAllBytes(savedPath, png);
        onScreenshotSaved?.Invoke(savedPath);
        Debug.Log("[AR] Screenshot saved locally: " + savedPath);
#endif

        RestoreUI(barOn, menuOn, banOn, toastOn);

        // Restore measurement label that was active before capture
        if (activeLabelPot != null)
            ShowLabelForPot(activeLabelPot);

        // Upload to backend via DesignUploadService so it appears on My Designs page
        if (DesignUploadService.Instance != null)
        {
            DesignUploadService.Instance.UploadDesign(
                png,
                designName,
                "AR Design",
                "front",
                DateTime.UtcNow.ToString("o"),
                "{}"
            );
        }
        else
        {
            // Fallback: upload directly if DesignUploadService is not in scene
            StartCoroutine(UploadToBackend(png, fn));
        }

        ShowToast("Screenshot Saved!");
    }

    // ════════════════════════════════════════════════════════════════
    //  READ FLUTTER INTENT EXTRAS (JWT + API URL)
    // ════════════════════════════════════════════════════════════════
    private void ReadIntentExtras()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using var intent = activity.Call<AndroidJavaObject>("getIntent");
            using var extras = intent.Call<AndroidJavaObject>("getExtras");
            if (extras != null)
            {
                _jwtToken = extras.Call<string>("getString", "jwt_token") ?? "";
                _apiBaseUrl = extras.Call<string>("getString", "api_base_url") ?? "";
            }
            Debug.Log($"[AR] Intent extras — baseUrl={_apiBaseUrl}, tokenPresent={!string.IsNullOrEmpty(_jwtToken)}");
        }
        catch (Exception e)
        {
            Debug.LogError("[AR] Failed to read intent extras: " + e.Message);
        }
#endif
    }

    // ════════════════════════════════════════════════════════════════
    //  UPLOAD TO BACKEND (fallback when DesignUploadService is absent)
    // ════════════════════════════════════════════════════════════════
    private IEnumerator UploadToBackend(byte[] pngData, string fileName)
    {
        // Try ApiClient first (proper auth + token refresh)
        if (ApiClient.Instance != null && ApiClient.Instance.IsAuthenticated)
        {
            string designName = Path.GetFileNameWithoutExtension(fileName);
            var fields = new Dictionary<string, string>
            {
                { "designName", designName },
                { "projectName", "AR Design" },
                { "designSide", "front" },
                { "clientTimestamp", DateTime.UtcNow.ToString("o") },
                { "metadataJson", "{}" }
            };
            ApiClient.Instance.UploadMultipart("/api/user/designs/upload", pngData, fileName, "file", fields, response =>
            {
                if (response.isSuccess)
                {
                    Debug.Log("[AR] Design uploaded via ApiClient: " + response.body);
                    ShowToast("Saved to My Designs!");
                }
                else
                {
                    Debug.LogWarning("[AR] Upload failed: " + response.error + " | HTTP " + response.statusCode);
                }
            });
            yield break;
        }

        // Last resort: raw upload with intent JWT
        if (string.IsNullOrEmpty(_jwtToken))
            ReadIntentExtras();

        if (string.IsNullOrEmpty(_jwtToken))
        {
            Debug.LogWarning("[AR] No JWT token — saved locally only.");
            yield break;
        }

        string baseUrl = _apiBaseUrl.TrimEnd('/');
        if (string.IsNullOrEmpty(baseUrl))
            baseUrl = "http://192.168.1.253:8081";

        string url = baseUrl + "/api/user/designs/upload";

        var form = new WWWForm();
        form.AddBinaryData("file", pngData, fileName ?? "ar_design.png", "image/png");
        form.AddField("designName", "AR Design " + DateTime.Now.ToString("yyyy-MM-dd HH:mm"));

        using var request = UnityWebRequest.Post(url, form);
        request.SetRequestHeader("Authorization", "Bearer " + _jwtToken);
        request.timeout = 15;

        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
        {
            Debug.Log("[AR] Design uploaded: " + request.downloadHandler.text);
            ShowToast("Saved to My Designs!");
        }
        else
        {
            Debug.LogError("[AR] Upload failed: " + request.error + " | HTTP " + request.responseCode);
        }
    }

    private void RestoreUI(bool bar, bool menu, bool banner, bool toast)
    {
        if (barGO)    barGO.SetActive(bar);
        if (menuGO)   menuGO.SetActive(menu);
        if (bannerGO) bannerGO.SetActive(banner);
        if (toastGO)  toastGO.SetActive(toast);
    }

    // ════════════════════════════════════════════════════════════════
    //  TOAST
    // ════════════════════════════════════════════════════════════════
    private void ShowToast(string msg)
    {
        if (!toastGO) return;
        if (toastCR != null) StopCoroutine(toastCR);
        if (toastTxt) toastTxt.text = msg;
        toastCR = StartCoroutine(ToastAnimation());
    }

    private IEnumerator ToastAnimation()
    {
        toastGO.SetActive(true);
        var cg = toastGO.GetComponent<CanvasGroup>();
        cg.alpha = 0;
        for (float t = 0; t < 0.2f; t += Time.unscaledDeltaTime)
        { cg.alpha = t / 0.2f; yield return null; }
        cg.alpha = 1;
        yield return new WaitForSecondsRealtime(2f);
        for (float t = 0; t < 0.3f; t += Time.unscaledDeltaTime)
        { cg.alpha = 1f - t / 0.3f; yield return null; }
        cg.alpha = 0;
        toastGO.SetActive(false);
        toastCR = null;
    }

    // ════════════════════════════════════════════════════════════════
    //  POT SPRITES
    // ════════════════════════════════════════════════════════════════
    private void LoadPotSprites()
    {
        spr1 = LoadSpr("potsphoto/Screenshot 2026-02-27 213635");
        if (spr1 == null) spr1 = LoadSpr("potsphoto/pot2");
        spr2 = LoadSpr("potsphoto/pot2 1");
    }

    private Sprite LoadSpr(string path)
    {
        var tex = Resources.Load<Texture2D>(path);
        if (tex == null) return null;
        return Sprite.Create(tex,
            new Rect(0, 0, tex.width, tex.height), new Vector2(0.5f, 0.5f));
    }

    // ════════════════════════════════════════════════════════════════
    //  CANVAS
    // ════════════════════════════════════════════════════════════════
    private Canvas CreateCanvas()
    {
        var existES = FindFirstObjectByType<EventSystem>(FindObjectsInactive.Include);
        if (existES != null) existES.transform.SetParent(null);

        foreach (var c in FindObjectsByType<Canvas>(FindObjectsSortMode.None))
            c.gameObject.SetActive(false);

        if (EventSystem.current == null)
        {
            if (existES != null) existES.gameObject.SetActive(true);
            else
            {
                var esGo = new GameObject("EventSystem");
                esGo.AddComponent<EventSystem>();
                esGo.AddComponent<UnityEngine.InputSystem.UI.InputSystemUIInputModule>();
            }
        }

        var go = new GameObject("ARCanvas");
        var cv = go.AddComponent<Canvas>();
        cv.renderMode = RenderMode.ScreenSpaceOverlay;
        cv.sortingOrder = 100;

        var sc = go.AddComponent<CanvasScaler>();
        sc.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        sc.referenceResolution = new Vector2(1080, 2160);
        sc.matchWidthOrHeight = 0.5f;

        go.AddComponent<GraphicRaycaster>();
        return cv;
    }

    // ════════════════════════════════════════════════════════════════
    //  UI SETUP
    // ════════════════════════════════════════════════════════════════
    private void SetupRuntimeUI()
    {
        Transform root = canvas.transform;
        BuildBottomBar(root);
        BuildExitButton(root);
        BuildDeleteBanner(root);
        BuildToast(root);
        BuildMenu(root);
    }

    private void BuildBottomBar(Transform root)
    {
        float barH = 200f;

        barGO = new GameObject("BottomBar");
        barGO.transform.SetParent(root, false);
        var rt = barGO.AddComponent<RectTransform>();
        rt.anchorMin = new Vector2(0, 0);
        rt.anchorMax = new Vector2(1, 0);
        rt.pivot     = new Vector2(0.5f, 0);
        rt.offsetMin = Vector2.zero;
        rt.offsetMax = new Vector2(0, barH);

        // Frosted dark bar
        var bg = barGO.AddComponent<Image>();
        bg.color = COL_BAR;
        bg.raycastTarget = true;

        // Soft shadow on top edge
        var shadowLine = new GameObject("ShadowEdge");
        shadowLine.transform.SetParent(barGO.transform, false);
        var slrt = shadowLine.AddComponent<RectTransform>();
        slrt.anchorMin = new Vector2(0, 1); slrt.anchorMax = new Vector2(1, 1);
        slrt.pivot = new Vector2(0.5f, 0); slrt.offsetMin = Vector2.zero;
        slrt.offsetMax = Vector2.zero; slrt.sizeDelta = new Vector2(0, 8f);
        var slImg = shadowLine.AddComponent<Image>();
        slImg.color = new Color(0, 0, 0, 0.15f);
        slImg.raycastTarget = false;

        float gap = 240f;
        float iconSz = 68f;  // BIGGER icons

        // ── Camera button (left) ──
        var camBtn = MakeBarIcon(barGO.transform, "CameraBtn", new Vector2(-gap, 8), 130);
        DrawCameraIcon3D(camBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(camBtn.transform, "Capture", new Vector2(0, -48f));
        camBtn.onClick.AddListener(TakeScreenshot);

        // ── Menu/Browse button (center) ──
        var menuBtn = MakeBarIcon(barGO.transform, "MenuBtn", new Vector2(0, 8), 140);
        DrawMenuGridIcon(menuBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(menuBtn.transform, "Menu", new Vector2(0, -48f));
        menuBtn.onClick.AddListener(() => SetMenuOpen(true));

        // ── Trash button (right) ──
        var trashBtn = MakeBarIcon(barGO.transform, "TrashBtn", new Vector2(gap, 8), 130);
        DrawTrashIcon3D(trashBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(trashBtn.transform, "Remove", new Vector2(0, -48f));
        trashBtn.onClick.AddListener(() => SetDeleteMode(!deleteMode));
    }

    private void MakeBarLabel(Transform parent, string text, Vector2 offset)
    {
        var go = new GameObject("Lbl");
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = offset;
        rt.sizeDelta = new Vector2(160, 36);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text;
        tmp.fontSize = 24f;
        tmp.fontStyle = FontStyles.Bold;
        tmp.color = new Color(ALOE.r, ALOE.g, ALOE.b, 0.85f);
        tmp.alignment = TextAlignmentOptions.Center;
        tmp.raycastTarget = false;
    }

    private void BuildExitButton(Transform root)
    {
        float safeT = SafeTopPx() + 22f;

        var go = new GameObject("ExitBtn");
        go.transform.SetParent(root, false);
        var rt = go.AddComponent<RectTransform>();
        // Anchored top-RIGHT
        rt.anchorMin = rt.anchorMax = new Vector2(1, 1);
        rt.pivot = new Vector2(1, 1);
        rt.anchoredPosition = new Vector2(-22, -safeT);
        rt.sizeDelta = new Vector2(90, 90);

        // Subtle frosted pill background
        var img = go.AddComponent<Image>();
        img.color = new Color(MOSS.r, MOSS.g, MOSS.b, 0.45f);
        img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);
        btn.onClick.AddListener(() => Application.Quit());

        // Soft shadow under pill
        go.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        go.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);

        // Forward arrow (pointing right →) in ALOE for contrast
        DrawForwardArrow3D(go.transform, 34f, ALOE, new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.5f));
    }

    private void BuildDeleteBanner(Transform root)
    {
        float safeT = SafeTopPx() + 18f;

        bannerGO = new GameObject("DeleteBanner");
        bannerGO.transform.SetParent(root, false);
        var rt = bannerGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(600, 72);

        // Frosted warm-red pill
        var bg = bannerGO.AddComponent<Image>();
        bg.color = COL_RED_BG;
        bg.raycastTarget = false;
        bannerGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        bannerGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);

        var txt = AddTMP(bannerGO.transform, "Txt",
            "Tap a plant to remove", 30f, CREAM,
            TextAlignmentOptions.Center);
        ((TextMeshProUGUI)txt).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)txt).characterSpacing = 1.5f;
        StretchFill(txt.gameObject);
    }

    private void BuildToast(Transform root)
    {
        float safeT = SafeTopPx() + 110f;

        toastGO = new GameObject("Toast");
        toastGO.transform.SetParent(root, false);
        var rt = toastGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(540, 68);

        // Frosted Cypress pill
        var bg = toastGO.AddComponent<Image>();
        bg.color = COL_TOAST_BG;
        bg.raycastTarget = false;
        toastGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.20f);
        toastGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);

        toastGO.AddComponent<CanvasGroup>();

        toastTxt = AddTMP(toastGO.transform, "Txt", "", 27f, CREAM,
            TextAlignmentOptions.Center);
        ((TextMeshProUGUI)toastTxt).fontStyle = FontStyles.Normal;
        ((TextMeshProUGUI)toastTxt).characterSpacing = 0.8f;
        StretchFill(toastTxt.gameObject);
    }

    private void BuildMenu(Transform root)
    {
        menuGO = new GameObject("MenuOverlay");
        menuGO.transform.SetParent(root, false);
        var rt = menuGO.AddComponent<RectTransform>();
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;

        // ── Warm CREAM full-screen background ──
        var bg = menuGO.AddComponent<Image>();
        bg.color = CREAM;
        bg.raycastTarget = true;

        float safeT = SafeTopPx();

        // ── Top header area ──
        var headerGO = new GameObject("Header");
        headerGO.transform.SetParent(menuGO.transform, false);
        var hrt = headerGO.AddComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 1); hrt.anchorMax = new Vector2(1, 1);
        hrt.pivot = new Vector2(0.5f, 1);
        hrt.offsetMin = Vector2.zero; hrt.offsetMax = Vector2.zero;
        hrt.sizeDelta = new Vector2(0, safeT + 280f);  // Taller header for bigger title

        // Subtle warm tint at top for depth
        var headerBg = headerGO.AddComponent<Image>();
        headerBg.color = new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.06f);
        headerBg.raycastTarget = false;

        // ── "Menu" title — EXTRA BIG, bold, Moss on cream ──
        var titleTmp = AddTMP(headerGO.transform, "Title",
            "Menu", 82f, MOSS, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)titleTmp).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)titleTmp).characterSpacing = 10f;
        var trt = titleTmp.GetComponent<RectTransform>();
        trt.anchorMin = new Vector2(0, 0); trt.anchorMax = new Vector2(1, 0);
        trt.pivot = new Vector2(0.5f, 0);
        trt.anchoredPosition = new Vector2(0, 52);
        trt.sizeDelta = new Vector2(-60, 100);

        // ── Subtitle — bigger, beneath title, darker ──
        var subTmp = AddTMP(headerGO.transform, "Subtitle",
            "Pick your plant, then tap to place", 30f,
            new Color(MOSS.r, MOSS.g, MOSS.b, 0.70f),
            TextAlignmentOptions.Center);
        ((TextMeshProUGUI)subTmp).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)subTmp).characterSpacing = 1.2f;
        var srt2 = subTmp.GetComponent<RectTransform>();
        srt2.anchorMin = new Vector2(0, 0); srt2.anchorMax = new Vector2(1, 0);
        srt2.pivot = new Vector2(0.5f, 1);
        srt2.anchoredPosition = new Vector2(0, 46);
        srt2.sizeDelta = new Vector2(-80, 42);

        // Thin separator line
        var sep = new GameObject("Sep");
        sep.transform.SetParent(headerGO.transform, false);
        var seprt = sep.AddComponent<RectTransform>();
        seprt.anchorMin = new Vector2(0.15f, 0); seprt.anchorMax = new Vector2(0.85f, 0);
        seprt.pivot = new Vector2(0.5f, 0);
        seprt.offsetMin = seprt.offsetMax = Vector2.zero;
        seprt.sizeDelta = new Vector2(0, 1.5f);
        sep.AddComponent<Image>().color = new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.20f);
        sep.GetComponent<Image>().raycastTarget = false;

        // ── Plant cards — images float, NO card backgrounds, pushed UP ──
        float imgSz  = 380f;
        float imgGap = 44f;
        float totalW = imgSz * 2 + imgGap;
        float x1 = -totalW / 2f + imgSz / 2f;
        float x2 = x1 + imgSz + imgGap;
        float cardsY = 200f;  // Pushed near the top

        var btn1 = MakePotImageButton(menuGO.transform, "Pot1Btn",
            spr1, new Vector2(x1, cardsY), imgSz, 0);
        sel1Img = btn1.GetComponent<Image>();

        MakePotNameLabel(menuGO.transform, "Name1",
            (potNames != null && potNames.Length > 0) ? potNames[0] : "Plant 1",
            new Vector2(x1, cardsY - imgSz / 2f - 30f));

        var btn2 = MakePotImageButton(menuGO.transform, "Pot2Btn",
            spr2, new Vector2(x2, cardsY), imgSz, 1);
        sel2Img = btn2.GetComponent<Image>();

        MakePotNameLabel(menuGO.transform, "Name2",
            (potNames != null && potNames.Length > 1) ? potNames[1] : "Plant 2",
            new Vector2(x2, cardsY - imgSz / 2f - 30f));

        // ── Done button — wide pill, Olive on cream ──
        var doneGO = new GameObject("DoneBtn");
        doneGO.transform.SetParent(menuGO.transform, false);
        var drt = doneGO.AddComponent<RectTransform>();
        drt.anchorMin = drt.anchorMax = new Vector2(0.5f, 0);
        drt.pivot = new Vector2(0.5f, 0);
        drt.anchoredPosition = new Vector2(0, 120);
        drt.sizeDelta = new Vector2(460, 96);

        var doneBg = doneGO.AddComponent<Image>();
        doneBg.color = OLIVE;
        doneBg.raycastTarget = true;
        doneGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        doneGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -5);

        var doneBtn = doneGO.AddComponent<Button>();
        doneBtn.targetGraphic = doneBg;
        NavNone(doneBtn);
        doneBtn.onClick.AddListener(() => SetMenuOpen(false));

        var doneTxt = AddTMP(doneGO.transform, "DoneTxt",
            "Place It", 34f, CREAM, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)doneTxt).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)doneTxt).characterSpacing = 3f;
        StretchFill(doneTxt.gameObject);

        HighlightSelection();
    }

    private void MakePotNameLabel(Transform parent, string name, string text, Vector2 pos)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos;
        rt.sizeDelta = new Vector2(380, 52);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text;
        tmp.fontSize = 26f;
        tmp.color = MOSS;
        tmp.alignment = TextAlignmentOptions.Center;
        tmp.fontStyle = FontStyles.Bold;
        tmp.characterSpacing = 1.5f;
        tmp.raycastTarget = false;
    }

    private Button MakePotImageButton(Transform parent, string name,
        Sprite spr, Vector2 pos, float size, int index)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos;
        rt.sizeDelta = new Vector2(size, size);

        // NO card background — image floats on the olive canvas
        var bgImg = go.AddComponent<Image>();
        bgImg.color = Color.clear;
        bgImg.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = bgImg;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);

        int idx = index;
        btn.onClick.AddListener(() =>
        {
            selectedPotIndex = idx;
            HighlightSelection();
        });

        if (spr != null)
        {
            var imgGO = new GameObject("Img");
            imgGO.transform.SetParent(go.transform, false);
            var irt = imgGO.AddComponent<RectTransform>();
            irt.anchorMin = new Vector2(0.04f, 0.04f);
            irt.anchorMax = new Vector2(0.96f, 0.96f);
            irt.offsetMin = irt.offsetMax = Vector2.zero;
            var img = imgGO.AddComponent<Image>();
            img.sprite = spr;
            img.preserveAspect = true;
            img.raycastTarget = false;
        }

        // No selection ring / background — selection is shown via scale animation

        return btn;
    }

    private void HighlightSelection()
    {
        HighlightCard(sel1Img, selectedPotIndex == 0);
        HighlightCard(sel2Img, selectedPotIndex == 1);
    }

    private void HighlightCard(Image card, bool selected)
    {
        if (!card) return;
        // NEVER put any background behind pot images
        card.color = Color.clear;
        // Remove any Outline — zero background artifacts
        var outline = card.GetComponent<Outline>();
        if (outline) outline.enabled = false;
        // SELECTION = SCALE ANIMATION: selected image grows slightly
        // to give a clear, clean "I picked this" feel — no backgrounds.
        var rt = card.GetComponent<RectTransform>();
        if (rt)
        {
            Vector3 target = selected ? new Vector3(1.10f, 1.10f, 1f)
                                      : Vector3.one;
            // Smooth animated scale via coroutine
            StopCoroutine("AnimateCardScale");
            StartCoroutine(AnimateCardScale(rt, target, 0.15f));
        }
    }

    private IEnumerator AnimateCardScale(RectTransform rt, Vector3 target, float duration)
    {
        Vector3 start = rt.localScale;
        for (float t = 0; t < duration; t += Time.unscaledDeltaTime)
        {
            float p = t / duration;
            // Ease-out cubic for a snappy feel
            float ease = 1f - Mathf.Pow(1f - p, 3f);
            rt.localScale = Vector3.LerpUnclamped(start, target, ease);
            yield return null;
        }
        rt.localScale = target;
    }

    private void SetMenuOpen(bool open)
    {
        if (menuGO)  menuGO.SetActive(open);
        if (barGO)   barGO.SetActive(!open);
        if (open) SetDeleteMode(false);
    }

    // ════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ════════════════════════════════════════════════════════════════
    private float SafeTopPx()
    {
        return (Screen.height - Screen.safeArea.yMax) / canvas.scaleFactor;
    }

    private TMP_Text AddTMP(Transform parent, string name, string text,
        float fontSize, Color color, TextAlignmentOptions align)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text      = text;
        tmp.fontSize  = fontSize;
        tmp.color     = color;
        tmp.alignment = align;
        tmp.raycastTarget = false;
        return tmp;
    }

    private void StretchFill(GameObject go)
    {
        var rt = go.GetComponent<RectTransform>();
        if (!rt) return;
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;
    }

    private Button MakeBarIcon(Transform parent, string name, Vector2 offset, float hitSize = 120)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.sizeDelta = new Vector2(hitSize, hitSize);
        rt.anchoredPosition = offset;

        // No visible background — transparent hit area only
        var img = go.AddComponent<Image>();
        img.color = Color.clear;
        img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);
        return btn;
    }

    private void NavNone(Button b)
    {
        var nav = b.navigation;
        nav.mode = Navigation.Mode.None;
        b.navigation = nav;
    }

    private GameObject MakeIconRect(Transform parent, string name,
        Vector2 size, Vector2 offset, Color color)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.sizeDelta = size;
        rt.anchoredPosition = offset;
        var img = go.AddComponent<Image>();
        img.color = color;
        img.raycastTarget = false;
        return go;
    }

    // ════════════════════════════════════════════════════════════════
    //  3D ICON DRAWING  (layered depth illusion, no backgrounds)
    // ════════════════════════════════════════════════════════════════

    /// <summary>Camera icon: body + viewfinder + lens circle with 3D shadow layers</summary>
    private void DrawCameraIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        // Shadow layer (offset down-right)
        MakeIconRect(p, "BodySh", new Vector2(sz * 0.92f, sz * 0.60f),
            new Vector2(sz * 0.03f, -sz * 0.07f), shadow);
        // Body
        MakeIconRect(p, "Body", new Vector2(sz * 0.92f, sz * 0.60f),
            new Vector2(0, -sz * 0.02f), front);
        // Viewfinder bump
        MakeIconRect(p, "VF", new Vector2(sz * 0.28f, sz * 0.16f),
            new Vector2(-sz * 0.12f, sz * 0.35f), front);
        MakeIconRect(p, "VFSh", new Vector2(sz * 0.28f, sz * 0.16f),
            new Vector2(-sz * 0.09f, sz * 0.33f), shadow);
        // Flash bump
        MakeIconRect(p, "Flash", new Vector2(sz * 0.10f, sz * 0.10f),
            new Vector2(sz * 0.30f, sz * 0.35f), front);
        // Lens outer ring
        MakeIconRect(p, "LensOuter", new Vector2(sz * 0.32f, sz * 0.32f),
            new Vector2(0, -sz * 0.02f), shadow);
        // Lens inner
        MakeIconRect(p, "LensInner", new Vector2(sz * 0.22f, sz * 0.22f),
            new Vector2(0, -sz * 0.02f), new Color(front.r, front.g, front.b, 0.45f));
        // Lens highlight dot
        MakeIconRect(p, "LensDot", new Vector2(sz * 0.08f, sz * 0.08f),
            new Vector2(sz * 0.04f, sz * 0.04f), front);
    }

    /// <summary>Grid/menu icon: 2×2 dots with 3D shadow</summary>
    private void DrawMenuGridIcon(Transform p, float sz, Color front, Color shadow)
    {
        float dot = sz * 0.22f;
        float sp  = sz * 0.17f;
        for (int r = 0; r < 2; r++)
        for (int c = 0; c < 2; c++)
        {
            float x = (c == 0 ? -sp : sp);
            float y = (r == 0 ? -sp : sp);
            // Shadow
            MakeIconRect(p, $"DSh{r}{c}", new Vector2(dot, dot),
                new Vector2(x + 2f, y - 2f), shadow);
            // Dot
            MakeIconRect(p, $"D{r}{c}", new Vector2(dot, dot),
                new Vector2(x, y), front);
        }
        // Center plus hint (tiny)
        MakeIconRect(p, "PlusH", new Vector2(sz * 0.06f, dot * 1.6f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
        MakeIconRect(p, "PlusV", new Vector2(dot * 1.6f, sz * 0.06f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
    }

    /// <summary>Trash icon with 3D layered depth</summary>
    private void DrawTrashIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        float top = sz * 0.24f;
        // Body shadow
        MakeIconRect(p, "BodySh", new Vector2(sz * 0.52f, sz * 0.54f),
            new Vector2(2f, top - sz * 0.36f - 2f), shadow);
        // Body
        MakeIconRect(p, "Body", new Vector2(sz * 0.52f, sz * 0.54f),
            new Vector2(0, top - sz * 0.36f), front);
        // Lid shadow
        MakeIconRect(p, "LidSh", new Vector2(sz * 0.72f, sz * 0.08f),
            new Vector2(2f, top - 2f), shadow);
        // Lid
        MakeIconRect(p, "Lid", new Vector2(sz * 0.72f, sz * 0.08f),
            new Vector2(0, top), front);
        // Handle
        MakeIconRect(p, "Handle", new Vector2(sz * 0.22f, sz * 0.12f),
            new Vector2(0, top + sz * 0.12f), front);
        // Body vertical lines (detail)
        for (int i = -1; i <= 1; i++)
        {
            MakeIconRect(p, "Line" + i, new Vector2(sz * 0.04f, sz * 0.36f),
                new Vector2(i * sz * 0.12f, top - sz * 0.36f),
                new Color(shadow.r, shadow.g, shadow.b, 0.5f));
        }
    }

    /// <summary>Forward arrow (pointing RIGHT → exit) with 3D depth, no background</summary>
    private void DrawForwardArrow3D(Transform p, float sz, Color front, Color shadow)
    {
        float thick = sz * 0.16f;
        // Shadow shaft
        MakeIconRect(p, "ShaftSh", new Vector2(sz * 0.55f, thick),
            new Vector2(-sz * 0.04f + 2f, -2f), shadow);
        // Shaft
        MakeIconRect(p, "Shaft", new Vector2(sz * 0.55f, thick),
            new Vector2(-sz * 0.04f, 0), front);
        // Shadow chevron arms
        var s1 = MakeIconRect(p, "Arm1Sh", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f + 2f, sz * 0.14f - 2f), shadow);
        s1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var s2 = MakeIconRect(p, "Arm2Sh", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f + 2f, -sz * 0.14f - 2f), shadow);
        s2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
        // Chevron arms (pointing right)
        var a1 = MakeIconRect(p, "Arm1", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f, sz * 0.14f), front);
        a1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var a2 = MakeIconRect(p, "Arm2", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f, -sz * 0.14f), front);
        a2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
    }

#if DEVELOPMENT_BUILD || UNITY_EDITOR
    private void OnGUI()
    {
        if (Debug.developerConsoleVisible)
            Debug.developerConsoleVisible = false;
    }
#endif
}
