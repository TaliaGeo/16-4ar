using System;
using System.Collections;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Owns all screen-space UI: bottom bar, menu overlay, delete banner,
/// toast notifications. Procedural construction isolated from game logic.
///
/// Future improvement: replace procedural code with a UI Toolkit UXML or
/// Canvas prefab. This class keeps the existing code-built approach but
/// isolates it so everything else is decoupled from UI internals.
/// </summary>
public class ARUIController : MonoBehaviour
{
    // ── Earthy Green palette (same as original) ─────────────────────
    private static readonly Color MOSS    = new Color(0.173f, 0.204f, 0.141f);
    private static readonly Color CYPRESS = new Color(0.298f, 0.345f, 0.243f);
    private static readonly Color CEDAR   = new Color(0.584f, 0.584f, 0.506f);
    private static readonly Color OLIVE   = new Color(0.463f, 0.502f, 0.392f);
    private static readonly Color ALOE    = new Color(0.855f, 0.871f, 0.847f);
    private static readonly Color CREAM   = new Color(0.94f, 0.93f, 0.90f);

    private static readonly Color COL_BAR      = new Color(MOSS.r, MOSS.g, MOSS.b, 0.92f);
    private static readonly Color COL_WHITE    = ALOE;
    private static readonly Color COL_MGREY    = CEDAR;
    private static readonly Color COL_RED_BG   = new Color(0.72f, 0.28f, 0.24f);
    private static readonly Color COL_TOAST_BG = new Color(CYPRESS.r, CYPRESS.g, CYPRESS.b, 0.94f);
    private static readonly int   ZTEST_LEQUAL = 4;

    // ── Runtime refs ────────────────────────────────────────────────
    private Canvas     _canvas;
    private GameObject _barGO, _menuGO, _bannerGO, _toastGO;
    private TMP_Text   _toastTxt;
    private Coroutine  _toastCR;
    private Image[]    _selImages;
    private int        _selectedIndex;
    private Image      _deleteBtnImage;

    private static readonly int ZTestMode = Shader.PropertyToID("unity_GUIZTestMode");

    // ── External callbacks ──────────────────────────────────────────
    public Action OnCaptureClicked  { get; set; }
    public Action OnMenuOpened      { get; set; }
    public Action OnMenuClosed      { get; set; }
    public Action<bool> OnDeleteToggled { get; set; }
    public Action<int>  OnPotSelected   { get; set; }
    public Action OnExitClicked     { get; set; }

    public bool IsMenuOpen => _menuGO && _menuGO.activeSelf;
    public int  SelectedIndex => _selectedIndex;

    // ── Public accessors for screenshot hide/restore ────────────────
    public GameObject BarGO    => _barGO;
    public GameObject MenuGO   => _menuGO;
    public GameObject BannerGO => _bannerGO;
    public GameObject ToastGO  => _toastGO;

    // ── Init ────────────────────────────────────────────────────────

    /// <summary>
    /// Build all UI. Call once from the orchestrator's Awake/Start.
    /// </summary>
    public void BuildUI(PlantCatalogEntry[] catalog)
    {
        _canvas = CreateCanvas();
        Transform root = _canvas.transform;

        BuildBottomBar(root);
        BuildExitButton(root);
        BuildDeleteBanner(root);
        BuildToast(root);
        BuildMenu(root, catalog);

        _menuGO.SetActive(false);
        _bannerGO.SetActive(false);
        _toastGO.SetActive(false);
    }

    // ── Public API ──────────────────────────────────────────────────

    public void ShowToast(string msg)
    {
        if (!_toastGO) return;
        if (_toastCR != null) StopCoroutine(_toastCR);
        if (_toastTxt) _toastTxt.text = msg;
        _toastCR = StartCoroutine(ToastAnimation());
    }

    public void SetMenuOpen(bool open)
    {
        if (_menuGO) _menuGO.SetActive(open);
        if (_barGO)  _barGO.SetActive(!open);
        if (open) { SetDeleteBanner(false); OnMenuOpened?.Invoke(); }
        else OnMenuClosed?.Invoke();
    }

    public void SetDeleteBanner(bool on)
    {
        if (_bannerGO) _bannerGO.SetActive(on);
        if (_deleteBtnImage)
            _deleteBtnImage.color = on ? COL_RED_BG : COL_BAR;
    }

    /// <summary>Snapshot then hide all screen-space UI for screenshot.</summary>
    public (bool bar, bool menu, bool banner, bool toast) HideAllUI()
    {
        bool bar    = _barGO  && _barGO.activeSelf;
        bool menu   = _menuGO && _menuGO.activeSelf;
        bool banner = _bannerGO && _bannerGO.activeSelf;
        bool toast  = _toastGO && _toastGO.activeSelf;
        if (_barGO)    _barGO.SetActive(false);
        if (_menuGO)   _menuGO.SetActive(false);
        if (_bannerGO) _bannerGO.SetActive(false);
        if (_toastGO)  _toastGO.SetActive(false);
        return (bar, menu, banner, toast);
    }

    public void RestoreUI(bool bar, bool menu, bool banner, bool toast)
    {
        if (_barGO)    _barGO.SetActive(bar);
        if (_menuGO)   _menuGO.SetActive(menu);
        if (_bannerGO) _bannerGO.SetActive(banner);
        if (_toastGO)  _toastGO.SetActive(toast);
    }

    // ── Measurement labels (world-space) ────────────────────────────

    public GameObject CreateMeasureLabel(GameObject pot, string[] ignoreNames)
    {
        Bounds wb = ARBoundsUtility.WorldBounds(pot, ignoreNames);
        if (wb.size == Vector3.zero) return null;

        Bounds lb = ARBoundsUtility.LocalBounds(pot, ignoreNames);
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
        labelGO.transform.position = wb.center + new Vector3(0, wb.extents.y + 0.08f, 0);

        // Card
        var card = new GameObject("Card");
        card.transform.SetParent(cvRT, false);
        var crt = card.AddComponent<RectTransform>();
        crt.anchorMin = new Vector2(0, 0.22f); crt.anchorMax = Vector2.one;
        crt.offsetMin = crt.offsetMax = Vector2.zero;
        var cardImg = card.AddComponent<Image>();
        cardImg.color = COL_WHITE; cardImg.raycastTarget = false;

        // Height text
        var hGO = new GameObject("HTxt");
        hGO.transform.SetParent(card.transform, false);
        var htmp = hGO.AddComponent<TextMeshProUGUI>();
        htmp.text = string.Format("H: {0:F1} cm", hCm);
        htmp.fontSize = 34; htmp.color = COL_MGREY;
        htmp.alignment = TextAlignmentOptions.Center; htmp.raycastTarget = false;
        var hrt = hGO.GetComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 0.5f); hrt.anchorMax = Vector2.one;
        hrt.offsetMin = hrt.offsetMax = Vector2.zero;

        // Width × Depth text
        var wGO = new GameObject("WDTxt");
        wGO.transform.SetParent(card.transform, false);
        var wtmp = wGO.AddComponent<TextMeshProUGUI>();
        wtmp.text = string.Format("W: {0:F1}  D: {1:F1} cm", wCm, dCm);
        wtmp.fontSize = 30; wtmp.color = COL_MGREY;
        wtmp.alignment = TextAlignmentOptions.Center; wtmp.raycastTarget = false;
        var wrt = wGO.GetComponent<RectTransform>();
        wrt.anchorMin = Vector2.zero; wrt.anchorMax = new Vector2(1, 0.5f);
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
        stemImg.color = COL_MGREY; stemImg.raycastTarget = false;

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
        headImg.color = COL_MGREY; headImg.raycastTarget = false;

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

    // ════════════════════════════════════════════════════════════════
    //  PRIVATE — Canvas + UI builders (identical to original)
    // ════════════════════════════════════════════════════════════════

    private Canvas CreateCanvas()
    {
        var existES = FindFirstObjectByType<UnityEngine.EventSystems.EventSystem>(
            FindObjectsInactive.Include);
        if (existES != null) existES.transform.SetParent(null);

        foreach (var c in FindObjectsByType<Canvas>(FindObjectsSortMode.None))
            c.gameObject.SetActive(false);

        if (UnityEngine.EventSystems.EventSystem.current == null)
        {
            if (existES != null) existES.gameObject.SetActive(true);
            else
            {
                var esGo = new GameObject("EventSystem");
                esGo.AddComponent<UnityEngine.EventSystems.EventSystem>();
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

    private float SafeTopPx()
    {
        return (Screen.height - Screen.safeArea.yMax) / _canvas.scaleFactor;
    }

    // ── Bottom Bar ──────────────────────────────────────────────────

    private void BuildBottomBar(Transform root)
    {
        float barH = 200f;
        _barGO = new GameObject("BottomBar");
        _barGO.transform.SetParent(root, false);
        var rt = _barGO.AddComponent<RectTransform>();
        rt.anchorMin = new Vector2(0, 0); rt.anchorMax = new Vector2(1, 0);
        rt.pivot = new Vector2(0.5f, 0); rt.offsetMin = Vector2.zero;
        rt.offsetMax = new Vector2(0, barH);

        var bg = _barGO.AddComponent<Image>();
        bg.color = COL_BAR; bg.raycastTarget = true;

        // Shadow edge
        var shadowLine = new GameObject("ShadowEdge");
        shadowLine.transform.SetParent(_barGO.transform, false);
        var slrt = shadowLine.AddComponent<RectTransform>();
        slrt.anchorMin = new Vector2(0, 1); slrt.anchorMax = new Vector2(1, 1);
        slrt.pivot = new Vector2(0.5f, 0); slrt.offsetMin = Vector2.zero;
        slrt.offsetMax = Vector2.zero; slrt.sizeDelta = new Vector2(0, 8f);
        shadowLine.AddComponent<Image>().color = new Color(0, 0, 0, 0.15f);

        float gap = 240f, iconSz = 68f;

        var camBtn = MakeBarIcon(_barGO.transform, "CameraBtn", new Vector2(-gap, 8), 130);
        DrawCameraIcon3D(camBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(camBtn.transform, "Capture", new Vector2(0, -48f));
        camBtn.onClick.AddListener(() => OnCaptureClicked?.Invoke());

        var menuBtn = MakeBarIcon(_barGO.transform, "MenuBtn", new Vector2(0, 8), 140);
        DrawMenuGridIcon(menuBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(menuBtn.transform, "Menu", new Vector2(0, -48f));
        menuBtn.onClick.AddListener(() => SetMenuOpen(true));

        var trashBtn = MakeBarIcon(_barGO.transform, "TrashBtn", new Vector2(gap, 8), 130);
        _deleteBtnImage = trashBtn.GetComponent<Image>();
        DrawTrashIcon3D(trashBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(trashBtn.transform, "Remove", new Vector2(0, -48f));
        trashBtn.onClick.AddListener(() => OnDeleteToggled?.Invoke(!(_bannerGO && _bannerGO.activeSelf)));
    }

    // ── Exit Button ─────────────────────────────────────────────────

    private void BuildExitButton(Transform root)
    {
        float safeT = SafeTopPx() + 22f;
        var go = new GameObject("ExitBtn");
        go.transform.SetParent(root, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(1, 1);
        rt.pivot = new Vector2(1, 1);
        rt.anchoredPosition = new Vector2(-22, -safeT);
        rt.sizeDelta = new Vector2(90, 90);

        var img = go.AddComponent<Image>();
        img.color = new Color(MOSS.r, MOSS.g, MOSS.b, 0.45f);
        img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img; btn.transition = Selectable.Transition.None;
        NavNone(btn);
        btn.onClick.AddListener(() => OnExitClicked?.Invoke());

        go.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        go.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);
        DrawForwardArrow3D(go.transform, 34f, ALOE, new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.5f));
    }

    // ── Delete Banner ───────────────────────────────────────────────

    private void BuildDeleteBanner(Transform root)
    {
        float safeT = SafeTopPx() + 18f;
        _bannerGO = new GameObject("DeleteBanner");
        _bannerGO.transform.SetParent(root, false);
        var rt = _bannerGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(600, 72);

        var bg = _bannerGO.AddComponent<Image>();
        bg.color = COL_RED_BG; bg.raycastTarget = false;
        _bannerGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        _bannerGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);

        var txt = AddTMP(_bannerGO.transform, "Txt",
            "Tap a plant to remove", 30f, CREAM, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)txt).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)txt).characterSpacing = 1.5f;
        StretchFill(txt.gameObject);
    }

    // ── Toast ───────────────────────────────────────────────────────

    private void BuildToast(Transform root)
    {
        float safeT = SafeTopPx() + 110f;
        _toastGO = new GameObject("Toast");
        _toastGO.transform.SetParent(root, false);
        var rt = _toastGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(540, 68);

        var bg = _toastGO.AddComponent<Image>();
        bg.color = COL_TOAST_BG; bg.raycastTarget = false;
        _toastGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.20f);
        _toastGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);
        _toastGO.AddComponent<CanvasGroup>();

        _toastTxt = AddTMP(_toastGO.transform, "Txt", "", 27f, CREAM,
            TextAlignmentOptions.Center);
        ((TextMeshProUGUI)_toastTxt).fontStyle = FontStyles.Normal;
        ((TextMeshProUGUI)_toastTxt).characterSpacing = 0.8f;
        StretchFill(_toastTxt.gameObject);
    }

    private IEnumerator ToastAnimation()
    {
        _toastGO.SetActive(true);
        var cg = _toastGO.GetComponent<CanvasGroup>();
        cg.alpha = 0;
        for (float t = 0; t < 0.2f; t += Time.unscaledDeltaTime)
        { cg.alpha = t / 0.2f; yield return null; }
        cg.alpha = 1;
        yield return new WaitForSecondsRealtime(2f);
        for (float t = 0; t < 0.3f; t += Time.unscaledDeltaTime)
        { cg.alpha = 1f - t / 0.3f; yield return null; }
        cg.alpha = 0;
        _toastGO.SetActive(false);
        _toastCR = null;
    }

    // ── Menu ────────────────────────────────────────────────────────

    private void BuildMenu(Transform root, PlantCatalogEntry[] catalog)
    {
        _menuGO = new GameObject("MenuOverlay");
        _menuGO.transform.SetParent(root, false);
        var rt = _menuGO.AddComponent<RectTransform>();
        rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;

        var bg = _menuGO.AddComponent<Image>();
        bg.color = CREAM; bg.raycastTarget = true;

        float safeT = SafeTopPx();

        // Header
        var headerGO = new GameObject("Header");
        headerGO.transform.SetParent(_menuGO.transform, false);
        var hrt = headerGO.AddComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 1); hrt.anchorMax = new Vector2(1, 1);
        hrt.pivot = new Vector2(0.5f, 1);
        hrt.offsetMin = Vector2.zero; hrt.offsetMax = Vector2.zero;
        hrt.sizeDelta = new Vector2(0, safeT + 280f);

        var headerBg = headerGO.AddComponent<Image>();
        headerBg.color = new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.06f);
        headerBg.raycastTarget = false;

        var titleTmp = AddTMP(headerGO.transform, "Title", "Menu", 82f, MOSS, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)titleTmp).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)titleTmp).characterSpacing = 10f;
        var trt = titleTmp.GetComponent<RectTransform>();
        trt.anchorMin = new Vector2(0, 0); trt.anchorMax = new Vector2(1, 0);
        trt.pivot = new Vector2(0.5f, 0);
        trt.anchoredPosition = new Vector2(0, 52); trt.sizeDelta = new Vector2(-60, 100);

        var subTmp = AddTMP(headerGO.transform, "Subtitle",
            "Pick your plant, then tap to place", 30f,
            new Color(MOSS.r, MOSS.g, MOSS.b, 0.70f), TextAlignmentOptions.Center);
        ((TextMeshProUGUI)subTmp).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)subTmp).characterSpacing = 1.2f;
        var srt2 = subTmp.GetComponent<RectTransform>();
        srt2.anchorMin = new Vector2(0, 0); srt2.anchorMax = new Vector2(1, 0);
        srt2.pivot = new Vector2(0.5f, 1);
        srt2.anchoredPosition = new Vector2(0, 46); srt2.sizeDelta = new Vector2(-80, 42);

        // Separator
        var sep = new GameObject("Sep");
        sep.transform.SetParent(headerGO.transform, false);
        var seprt = sep.AddComponent<RectTransform>();
        seprt.anchorMin = new Vector2(0.15f, 0); seprt.anchorMax = new Vector2(0.85f, 0);
        seprt.pivot = new Vector2(0.5f, 0);
        seprt.offsetMin = seprt.offsetMax = Vector2.zero;
        seprt.sizeDelta = new Vector2(0, 1.5f);
        sep.AddComponent<Image>().color = new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.20f);
        sep.GetComponent<Image>().raycastTarget = false;

        // Plant cards — dynamic from catalog
        int count = catalog != null ? catalog.Length : 0;
        float imgSz = 380f, imgGap = 44f;
        float totalW = imgSz * count + imgGap * Mathf.Max(0, count - 1);
        float startX = -totalW / 2f + imgSz / 2f;
        float cardsY = 200f;

        _selImages = new Image[count];
        for (int i = 0; i < count; i++)
        {
            float x = startX + i * (imgSz + imgGap);
            string entryName = catalog[i] != null ? catalog[i].displayName : $"Plant {i + 1}";
            Sprite spr = catalog[i] != null ? catalog[i].menuSprite : null;

            var btn = MakePotImageButton(_menuGO.transform, $"Pot{i}Btn", spr, new Vector2(x, cardsY), imgSz, i);
            _selImages[i] = btn.GetComponent<Image>();
            MakePotNameLabel(_menuGO.transform, $"Name{i}", entryName, new Vector2(x, cardsY - imgSz / 2f - 30f));
        }

        // Done button
        var doneGO = new GameObject("DoneBtn");
        doneGO.transform.SetParent(_menuGO.transform, false);
        var drt = doneGO.AddComponent<RectTransform>();
        drt.anchorMin = drt.anchorMax = new Vector2(0.5f, 0);
        drt.pivot = new Vector2(0.5f, 0);
        drt.anchoredPosition = new Vector2(0, 120);
        drt.sizeDelta = new Vector2(460, 96);

        var doneBg = doneGO.AddComponent<Image>();
        doneBg.color = OLIVE; doneBg.raycastTarget = true;
        doneGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        doneGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -5);

        var doneBtn = doneGO.AddComponent<Button>();
        doneBtn.targetGraphic = doneBg; NavNone(doneBtn);
        doneBtn.onClick.AddListener(() => SetMenuOpen(false));

        var doneTxt = AddTMP(doneGO.transform, "DoneTxt", "Place It", 34f, CREAM, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)doneTxt).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)doneTxt).characterSpacing = 3f;
        StretchFill(doneTxt.gameObject);

        HighlightSelection();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void MakeBarLabel(Transform parent, string text, Vector2 offset)
    {
        var go = new GameObject("Lbl");
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = offset; rt.sizeDelta = new Vector2(160, 36);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text; tmp.fontSize = 24f; tmp.fontStyle = FontStyles.Bold;
        tmp.color = new Color(ALOE.r, ALOE.g, ALOE.b, 0.85f);
        tmp.alignment = TextAlignmentOptions.Center; tmp.raycastTarget = false;
    }

    private void MakePotNameLabel(Transform parent, string name, string text, Vector2 pos)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos; rt.sizeDelta = new Vector2(380, 52);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text; tmp.fontSize = 26f; tmp.color = MOSS;
        tmp.alignment = TextAlignmentOptions.Center;
        tmp.fontStyle = FontStyles.Bold;
        tmp.characterSpacing = 1.5f; tmp.raycastTarget = false;
    }

    private Button MakePotImageButton(Transform parent, string name,
        Sprite spr, Vector2 pos, float size, int index)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos; rt.sizeDelta = new Vector2(size, size);

        var bgImg = go.AddComponent<Image>();
        bgImg.color = Color.clear; bgImg.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = bgImg;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);

        int idx = index;
        btn.onClick.AddListener(() =>
        {
            _selectedIndex = idx;
            HighlightSelection();
            OnPotSelected?.Invoke(idx);
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
            img.sprite = spr; img.preserveAspect = true; img.raycastTarget = false;
        }

        return btn;
    }

    private void HighlightSelection()
    {
        if (_selImages == null) return;
        for (int i = 0; i < _selImages.Length; i++)
            HighlightCard(_selImages[i], _selectedIndex == i);
    }

    private void HighlightCard(Image card, bool selected)
    {
        if (!card) return;
        card.color = Color.clear;
        var outline = card.GetComponent<Outline>();
        if (outline) outline.enabled = false;
        var grt = card.GetComponent<RectTransform>();
        if (grt)
        {
            Vector3 target = selected ? new Vector3(1.10f, 1.10f, 1f) : Vector3.one;
            StartCoroutine(AnimateCardScale(grt, target, 0.15f));
        }
    }

    private IEnumerator AnimateCardScale(RectTransform rt, Vector3 target, float duration)
    {
        Vector3 start = rt.localScale;
        for (float t = 0; t < duration; t += Time.unscaledDeltaTime)
        {
            float p = t / duration;
            float ease = 1f - Mathf.Pow(1f - p, 3f);
            rt.localScale = Vector3.LerpUnclamped(start, target, ease);
            yield return null;
        }
        rt.localScale = target;
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

        var img = go.AddComponent<Image>();
        img.color = Color.clear; img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);
        return btn;
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
        tmp.text = text; tmp.fontSize = fontSize;
        tmp.color = color; tmp.alignment = align; tmp.raycastTarget = false;
        return tmp;
    }

    private void StretchFill(GameObject go)
    {
        var rt = go.GetComponent<RectTransform>();
        if (!rt) return;
        rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;
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
        rt.sizeDelta = size; rt.anchoredPosition = offset;
        var img = go.AddComponent<Image>();
        img.color = color;
        img.raycastTarget = false;
        return go;
    }

    // ── 3D Icon Drawing ─────────────────────────────────────────────

    private void DrawCameraIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        MakeIconRect(p, "BodySh", new Vector2(sz*0.92f, sz*0.60f), new Vector2(sz*0.03f, -sz*0.07f), shadow);
        MakeIconRect(p, "Body",   new Vector2(sz*0.92f, sz*0.60f), new Vector2(0, -sz*0.02f), front);
        MakeIconRect(p, "VF",     new Vector2(sz*0.28f, sz*0.16f), new Vector2(-sz*0.12f, sz*0.35f), front);
        MakeIconRect(p, "VFSh",   new Vector2(sz*0.28f, sz*0.16f), new Vector2(-sz*0.09f, sz*0.33f), shadow);
        MakeIconRect(p, "Flash",  new Vector2(sz*0.10f, sz*0.10f), new Vector2(sz*0.30f, sz*0.35f), front);
        MakeIconRect(p, "LensOuter", new Vector2(sz*0.32f, sz*0.32f), new Vector2(0, -sz*0.02f), shadow);
        MakeIconRect(p, "LensInner", new Vector2(sz*0.22f, sz*0.22f), new Vector2(0, -sz*0.02f),
            new Color(front.r, front.g, front.b, 0.45f));
        MakeIconRect(p, "LensDot", new Vector2(sz*0.08f, sz*0.08f), new Vector2(sz*0.04f, sz*0.04f), front);
    }

    private void DrawMenuGridIcon(Transform p, float sz, Color front, Color shadow)
    {
        float dot = sz * 0.22f, sp = sz * 0.17f;
        for (int r = 0; r < 2; r++)
        for (int c = 0; c < 2; c++)
        {
            float x = c == 0 ? -sp : sp, y = r == 0 ? -sp : sp;
            MakeIconRect(p, $"DSh{r}{c}", new Vector2(dot, dot), new Vector2(x + 2f, y - 2f), shadow);
            MakeIconRect(p, $"D{r}{c}",   new Vector2(dot, dot), new Vector2(x, y), front);
        }
        MakeIconRect(p, "PlusH", new Vector2(sz*0.06f, dot*1.6f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
        MakeIconRect(p, "PlusV", new Vector2(dot*1.6f, sz*0.06f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
    }

    private void DrawTrashIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        float top = sz * 0.24f;
        MakeIconRect(p, "BodySh",  new Vector2(sz*0.52f, sz*0.54f), new Vector2(2f, top-sz*0.36f-2f), shadow);
        MakeIconRect(p, "Body",    new Vector2(sz*0.52f, sz*0.54f), new Vector2(0, top-sz*0.36f), front);
        MakeIconRect(p, "LidSh",   new Vector2(sz*0.72f, sz*0.08f), new Vector2(2f, top-2f), shadow);
        MakeIconRect(p, "Lid",     new Vector2(sz*0.72f, sz*0.08f), new Vector2(0, top), front);
        MakeIconRect(p, "Handle",  new Vector2(sz*0.22f, sz*0.12f), new Vector2(0, top+sz*0.12f), front);
        for (int i = -1; i <= 1; i++)
            MakeIconRect(p, "Line"+i, new Vector2(sz*0.04f, sz*0.36f),
                new Vector2(i*sz*0.12f, top-sz*0.36f),
                new Color(shadow.r, shadow.g, shadow.b, 0.5f));
    }

    private void DrawForwardArrow3D(Transform p, float sz, Color front, Color shadow)
    {
        float thick = sz * 0.16f;
        MakeIconRect(p, "ShaftSh", new Vector2(sz*0.55f, thick), new Vector2(-sz*0.04f+2f, -2f), shadow);
        MakeIconRect(p, "Shaft",   new Vector2(sz*0.55f, thick), new Vector2(-sz*0.04f, 0), front);
        var s1 = MakeIconRect(p, "Arm1Sh", new Vector2(sz*0.44f, thick), new Vector2(sz*0.16f+2f, sz*0.14f-2f), shadow);
        s1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var s2 = MakeIconRect(p, "Arm2Sh", new Vector2(sz*0.44f, thick), new Vector2(sz*0.16f+2f, -sz*0.14f-2f), shadow);
        s2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
        var a1 = MakeIconRect(p, "Arm1", new Vector2(sz*0.44f, thick), new Vector2(sz*0.16f, sz*0.14f), front);
        a1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var a2 = MakeIconRect(p, "Arm2", new Vector2(sz*0.44f, thick), new Vector2(sz*0.16f, -sz*0.14f), front);
        a2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
    }
}
