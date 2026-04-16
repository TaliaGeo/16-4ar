using UnityEngine;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine.SceneManagement;
using UnityEngine.XR.ARFoundation;
using System.IO;

/// <summary>
/// Editor utility: one-click AND automatic setup for the modular AR system.
///   • Menu → AR → Setup Scene — manual trigger
///   • Runs automatically before every Build (IPreprocessBuildWithReport)
///   • Runs automatically when entering Play mode
/// Adds all required components and creates config + catalog assets.
/// </summary>
[InitializeOnLoad]
public class ARSceneSetup : IPreprocessBuildWithReport
{
    private const string ConfigPath   = "Assets/Settings/ARPlacementConfig.asset";
    private const string CatalogDir   = "Assets/Settings";

    // Known pot assets (from the original ARPlacementManager scene data)
    private static readonly string[] PrefabPaths =
    {
        "Assets/MobileARTemplateAssets/Prefabs/plant-pot/source/Plant_Pot_1.fbx",
        "Assets/MobileARTemplateAssets/Prefabs/potted-plant-02/source/potted_plant_2.fbx",
    };
    private static readonly string[] PotNames  = { "Classic Pot", "Modern Pot" };
    private static readonly float[]  PotHeights = { 30f, 25f };
    private static readonly string[] SpritePaths =
    {
        "Assets/potsphoto/Screenshot 2026-02-27 213635.png",
        "Assets/potsphoto/pot2 1.png",
    };

    // ════════════════════════════════════════════════════════════════
    //  AUTO-RUN: before Build
    // ════════════════════════════════════════════════════════════════

    public int callbackOrder => -10;

    public void OnPreprocessBuild(BuildReport report)
    {
        // Open every enabled build scene and ensure it's configured
        var scenes = EditorBuildSettings.scenes;
        string originalScenePath = SceneManager.GetActiveScene().path;

        foreach (var bs in scenes)
        {
            if (!bs.enabled || string.IsNullOrEmpty(bs.path)) continue;

            var scene = EditorSceneManager.OpenScene(bs.path, OpenSceneMode.Single);
            if (!SceneNeedsSetup()) continue;

            Debug.Log($"[ARSceneSetup] Auto-configuring build scene: {bs.path}");
            SetupCurrentScene(silent: true);
            EditorSceneManager.SaveScene(scene);
        }

        // Re-open the original scene so the editor state isn't disrupted
        if (!string.IsNullOrEmpty(originalScenePath) && File.Exists(originalScenePath))
            EditorSceneManager.OpenScene(originalScenePath, OpenSceneMode.Single);
    }

    // ════════════════════════════════════════════════════════════════
    //  AUTO-RUN: before entering Play mode
    // ════════════════════════════════════════════════════════════════

    static ARSceneSetup()
    {
        EditorApplication.playModeStateChanged += OnPlayModeChanged;
    }

    private static void OnPlayModeChanged(PlayModeStateChange state)
    {
        if (state != PlayModeStateChange.ExitingEditMode) return;
        if (!SceneNeedsSetup()) return;

        Debug.Log("[ARSceneSetup] Auto-configuring scene before Play...");
        SetupCurrentScene(silent: true);
        EditorSceneManager.SaveOpenScenes();
    }

    // ════════════════════════════════════════════════════════════════
    //  MANUAL: Menu → AR → Setup Scene
    // ════════════════════════════════════════════════════════════════

    [MenuItem("AR/Setup Scene", false, 1)]
    public static void SetupSceneMenu()
    {
        SetupCurrentScene(silent: false);
    }

    // ════════════════════════════════════════════════════════════════
    //  CORE SETUP (used by all entry points)
    // ════════════════════════════════════════════════════════════════

    private static bool SceneNeedsSetup()
    {
        var xro = FindXROrigin();
        if (xro == null) return false; // not an AR scene — nothing to do
        return xro.GetComponent<ARPlacementController>() == null;
    }

    private static void SetupCurrentScene(bool silent)
    {
        // ── 1. Find XR Origin ───────────────────────────────────────
        GameObject xrOrigin = FindXROrigin();
        if (xrOrigin == null)
        {
            string msg = "No XR Origin found in scene.";
            Debug.LogError("[ARSceneSetup] " + msg);
            if (!silent)
                EditorUtility.DisplayDialog("AR Scene Setup", msg + "\n\nAdd one via: GameObject → XR → XR Origin (AR)", "OK");
            return;
        }
        Debug.Log("[ARSceneSetup] Found XR Origin: " + xrOrigin.name);

        // ── 2. Remove missing scripts (old ARPlacementManager) ──────
        int removed = GameObjectUtility.RemoveMonoBehavioursWithMissingScript(xrOrigin);
        if (removed > 0)
            Debug.Log($"[ARSceneSetup] Removed {removed} missing script(s) from {xrOrigin.name}");

        // ── 3. Ensure AR managers ───────────────────────────────────
        EnsureComponent<ARRaycastManager>(xrOrigin);
        EnsureComponent<ARPlaneManager>(xrOrigin);
        EnsureComponent<ARAnchorManager>(xrOrigin);

        // ── 4. Create / find ARPlacementConfig ──────────────────────
        ARPlacementConfig config = EnsureConfig();

        // ── 5. Create / find PlantCatalogEntry assets ───────────────
        PlantCatalogEntry[] catalog = EnsureCatalog();

        // ── 6. Add ARSessionBootstrap ───────────────────────────────
        var bootstrap = EnsureComponent<ARSessionBootstrap>(xrOrigin);
        var bSO = new SerializedObject(bootstrap);
        var bConfig = bSO.FindProperty("config");
        if (bConfig != null && bConfig.objectReferenceValue == null)
        {
            bConfig.objectReferenceValue = config;
            bSO.ApplyModifiedProperties();
            Debug.Log("[ARSceneSetup] Assigned config to ARSessionBootstrap");
        }

        // ── 7. Add ARPlacementController ────────────────────────────
        var controller = EnsureComponent<ARPlacementController>(xrOrigin);
        var cSO = new SerializedObject(controller);

        // Always assign config + catalog (even if already set, to ensure correct refs)
        var cConfig = cSO.FindProperty("config");
        if (cConfig != null) cConfig.objectReferenceValue = config;

        var cCatalog = cSO.FindProperty("catalog");
        if (cCatalog != null)
        {
            cCatalog.arraySize = catalog.Length;
            for (int i = 0; i < catalog.Length; i++)
                cCatalog.GetArrayElementAtIndex(i).objectReferenceValue = catalog[i];
        }

        var rmProp = cSO.FindProperty("raycastManager");
        if (rmProp != null) rmProp.objectReferenceValue = xrOrigin.GetComponent<ARRaycastManager>();

        var pmProp = cSO.FindProperty("planeManager");
        if (pmProp != null) pmProp.objectReferenceValue = xrOrigin.GetComponent<ARPlaneManager>();

        var amProp = cSO.FindProperty("anchorManager");
        if (amProp != null) amProp.objectReferenceValue = xrOrigin.GetComponent<ARAnchorManager>();

        // Try to assign camera
        var camProp = cSO.FindProperty("arCamera");
        if (camProp != null && camProp.objectReferenceValue == null)
        {
            var cam = xrOrigin.GetComponentInChildren<Camera>();
            if (cam != null) camProp.objectReferenceValue = cam;
        }

        cSO.ApplyModifiedProperties();

        // ── 8. Mark scene dirty ─────────────────────────────────────
        EditorSceneManager.MarkSceneDirty(SceneManager.GetActiveScene());

        // ── Summary ─────────────────────────────────────────────────
        Debug.Log("══════════════════════════════════════════════════");
        Debug.Log("[ARSceneSetup] DONE — Scene is ready.");
        Debug.Log($"  XR Origin:     {xrOrigin.name}");
        Debug.Log($"  Config:        {AssetDatabase.GetAssetPath(config)}");
        Debug.Log($"  Catalog:       {catalog.Length} plant(s)");
        for (int i = 0; i < catalog.Length; i++)
            Debug.Log($"    [{i}] {catalog[i].displayName} — prefab={(catalog[i].prefab ? catalog[i].prefab.name : "MISSING")}, sprite={(catalog[i].menuSprite ? "OK" : "MISSING")}");
        Debug.Log("══════════════════════════════════════════════════");

        if (!silent)
        {
            EditorUtility.DisplayDialog("AR Scene Setup",
                $"Scene configured successfully!\n\n" +
                $"• XR Origin: {xrOrigin.name}\n" +
                $"• Config: {AssetDatabase.GetAssetPath(config)}\n" +
                $"• Catalog: {catalog.Length} plant(s)\n\n" +
                $"Save the scene (Ctrl+S), then Build.", "OK");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private static GameObject FindXROrigin()
    {
        var xro = Object.FindFirstObjectByType<Unity.XR.CoreUtils.XROrigin>();
        if (xro != null) return xro.gameObject;

        var rm = Object.FindFirstObjectByType<ARRaycastManager>();
        if (rm != null) return rm.gameObject;

        var pm = Object.FindFirstObjectByType<ARPlaneManager>();
        if (pm != null) return pm.gameObject;

        return null;
    }

    private static T EnsureComponent<T>(GameObject go) where T : Component
    {
        var c = go.GetComponent<T>();
        if (c == null)
        {
            c = go.AddComponent<T>();
            Debug.Log($"[ARSceneSetup] Added {typeof(T).Name} to {go.name}");
        }
        return c;
    }

    private static ARPlacementConfig EnsureConfig()
    {
        var existing = AssetDatabase.LoadAssetAtPath<ARPlacementConfig>(ConfigPath);
        if (existing != null) return existing;

        string dir = Path.GetDirectoryName(ConfigPath);
        if (!AssetDatabase.IsValidFolder(dir))
        {
            Directory.CreateDirectory(Path.Combine(Application.dataPath, "..", dir));
            AssetDatabase.Refresh();
        }

        var config = ScriptableObject.CreateInstance<ARPlacementConfig>();
        AssetDatabase.CreateAsset(config, ConfigPath);
        AssetDatabase.SaveAssets();
        Debug.Log("[ARSceneSetup] Created config: " + ConfigPath);
        return config;
    }

    private static PlantCatalogEntry[] EnsureCatalog()
    {
        var entries = new PlantCatalogEntry[PrefabPaths.Length];

        if (!AssetDatabase.IsValidFolder(CatalogDir))
        {
            Directory.CreateDirectory(Path.Combine(Application.dataPath, "..", CatalogDir));
            AssetDatabase.Refresh();
        }

        for (int i = 0; i < PrefabPaths.Length; i++)
        {
            string assetPath = $"{CatalogDir}/{PotNames[i].Replace(" ", "")}_CatalogEntry.asset";

            var existing = AssetDatabase.LoadAssetAtPath<PlantCatalogEntry>(assetPath);
            if (existing != null) { entries[i] = existing; continue; }

            var entry = ScriptableObject.CreateInstance<PlantCatalogEntry>();
            entry.displayName = PotNames[i];
            entry.realHeightCm = PotHeights[i];
            entry.ignoreRendererNames = new[] { "ground", "shadow", "plane" };

            // Load prefab — try proper .prefab first, then FBX source
            entry.prefab = AssetDatabase.LoadAssetAtPath<GameObject>(
                $"Assets/MyPots/Prefabs/{Path.GetFileNameWithoutExtension(PrefabPaths[i])}.prefab");
            if (entry.prefab == null)
                entry.prefab = AssetDatabase.LoadAssetAtPath<GameObject>(PrefabPaths[i]);
            if (entry.prefab == null)
                Debug.LogWarning($"[ARSceneSetup] Prefab not found for {PotNames[i]}");

            // Load sprite
            if (i < SpritePaths.Length)
            {
                var importer = AssetImporter.GetAtPath(SpritePaths[i]) as TextureImporter;
                if (importer != null && importer.textureType != TextureImporterType.Sprite)
                {
                    importer.textureType = TextureImporterType.Sprite;
                    importer.spriteImportMode = SpriteImportMode.Single;
                    importer.SaveAndReimport();
                }
                entry.menuSprite = AssetDatabase.LoadAssetAtPath<Sprite>(SpritePaths[i]);
                if (entry.menuSprite == null)
                    Debug.LogWarning($"[ARSceneSetup] Sprite not found at {SpritePaths[i]}");
            }

            AssetDatabase.CreateAsset(entry, assetPath);
            Debug.Log($"[ARSceneSetup] Created catalog entry: {assetPath}");
            entries[i] = entry;
        }

        AssetDatabase.SaveAssets();
        return entries;
    }
}
