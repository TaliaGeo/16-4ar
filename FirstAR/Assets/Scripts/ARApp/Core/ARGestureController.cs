using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.EnhancedTouch;
using ETouch = UnityEngine.InputSystem.EnhancedTouch.Touch;
using TouchPhaseIS = UnityEngine.InputSystem.TouchPhase;

/// <summary>
/// Converts raw Enhanced Touch input into semantic AR gestures.
/// Raises C# events consumed by ARPlacementController.
/// </summary>
public class ARGestureController : MonoBehaviour
{
    [SerializeField] private Camera arCamera;

    /// <summary>Tap on empty surface: (screenPos)</summary>
    public event Action<Vector2> OnSurfaceTap;

    /// <summary>Tap on an existing placed object root.</summary>
    public event Action<GameObject> OnObjectTap;

    /// <summary>Pinch began on a target: (target, startDist, startScale)</summary>
    public event Action<GameObject, float, Vector3> OnPinchBegan;

    /// <summary>Pinch moved: (target, ratio)</summary>
    public event Action<GameObject, float> OnPinchMoved;

    /// <summary>Pinch ended: (target)</summary>
    public event Action<GameObject> OnPinchEnded;

    private float _lastTapTime = -10f;
    private float _tapCooldown = 0.22f;

    private float      _pinchStartDist;
    private Vector3    _pinchStartScale;
    private GameObject _pinchTarget;
    private bool       _pinchActive;
    private bool       _deleteMode;

    private readonly List<RaycastResult> _uiHits = new List<RaycastResult>();

    /// <summary>Set by the controller when delete mode is toggled.</summary>
    public bool DeleteMode { get => _deleteMode; set => _deleteMode = value; }

    /// <summary>The object's localScale when the current pinch started.</summary>
    public Vector3 PinchStartScale => _pinchStartScale;

    /// <summary>External lookup: given a collider transform, find the spawned root.</summary>
    public Func<Transform, GameObject> FindSpawnedRoot { get; set; }

    /// <summary>External list reference (read-only from gesture perspective).</summary>
    public List<GameObject> Spawned { get; set; }

    public void Init(Camera cam, float tapCooldown)
    {
        arCamera = cam;
        _tapCooldown = tapCooldown;
    }

    private void Update()
    {
        HandleTouch();
        HandlePinch();
    }

    // ── Tap ─────────────────────────────────────────────────────────

    private void HandleTouch()
    {
        Vector2 pos = Vector2.zero;
        bool tap = false;

        var touches = ETouch.activeTouches;
        if (touches.Count == 1 && touches[0].phase == TouchPhaseIS.Began)
        {
            pos = touches[0].screenPosition;
            tap = true;
        }

#if UNITY_EDITOR
        if (!tap && Mouse.current != null && Mouse.current.leftButton.wasPressedThisFrame)
        {
            pos = Mouse.current.position.ReadValue();
            tap = true;
        }
#endif

        if (!tap) return;
        if (IsOverUI(pos)) return;
        if (Time.unscaledTime - _lastTapTime < _tapCooldown) return;
        _lastTapTime = Time.unscaledTime;

        // Raycast against placed objects first
        Ray ray = arCamera.ScreenPointToRay(pos);
        if (Physics.Raycast(ray, out RaycastHit hit, 100f))
        {
            GameObject tapped = FindSpawnedRoot?.Invoke(hit.collider.transform);
            if (tapped != null)
            {
                OnObjectTap?.Invoke(tapped);
                return;
            }
        }

        if (_deleteMode) return;

        // Surface tap
        OnSurfaceTap?.Invoke(pos);
    }

    // ── Pinch ───────────────────────────────────────────────────────

    private void HandlePinch()
    {
        var touches = ETouch.activeTouches;

        // Detect pinch end: was active but no longer 2 fingers
        if (_pinchActive && touches.Count != 2)
        {
            _pinchActive = false;
            if (_pinchTarget != null)
            {
                OnPinchEnded?.Invoke(_pinchTarget);
                _pinchTarget = null;
            }
            return;
        }

        if (touches.Count != 2 || _deleteMode) return;

        var t0 = touches[0];
        var t1 = touches[1];
        float dist = Vector2.Distance(t0.screenPosition, t1.screenPosition);

        if (t0.phase == TouchPhaseIS.Began || t1.phase == TouchPhaseIS.Began)
        {
            Vector2 mid = (t0.screenPosition + t1.screenPosition) * 0.5f;
            Ray ray = arCamera.ScreenPointToRay(mid);
            _pinchTarget = null;
            Physics.SyncTransforms();
            if (Physics.Raycast(ray, out RaycastHit hit, 100f))
                _pinchTarget = FindSpawnedRoot?.Invoke(hit.collider.transform);
            if (_pinchTarget == null && Spawned != null && Spawned.Count > 0)
                _pinchTarget = Spawned[Spawned.Count - 1];
            if (_pinchTarget == null) return;

            _pinchStartDist  = dist;
            _pinchStartScale = _pinchTarget.transform.localScale;
            _pinchActive = true;
            OnPinchBegan?.Invoke(_pinchTarget, _pinchStartDist, _pinchStartScale);
            return;
        }

        if (_pinchTarget == null || _pinchStartDist < 1f) return;

        float ratio = dist / _pinchStartDist;
        OnPinchMoved?.Invoke(_pinchTarget, ratio);
    }

    // ── UI check ────────────────────────────────────────────────────

    private bool IsOverUI(Vector2 screenPos)
    {
        if (EventSystem.current == null) return false;
        var ped = new PointerEventData(EventSystem.current) { position = screenPos };
        _uiHits.Clear();
        EventSystem.current.RaycastAll(ped, _uiHits);
        for (int i = 0; i < _uiHits.Count; i++)
        {
            var go = _uiHits[i].gameObject;
            if (go.layer == 5) return true;
            if (go.GetComponentInParent<UnityEngine.UI.Button>()) return true;
        }
        return false;
    }
}
