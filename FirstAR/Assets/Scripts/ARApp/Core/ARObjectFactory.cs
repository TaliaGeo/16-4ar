using UnityEngine;

/// <summary>
/// Instantiates a prefab under a parent transform, normalizes its height,
/// strips disallowed physics/interaction components, forces opaque/always-visible,
/// isolates on a physics layer, fits a trigger collider, and attaches ARObjectController.
/// </summary>
public static class ARObjectFactory
{
    /// <summary>
    /// Spawn a new object from a catalog entry under the given parent.
    /// Returns the root GameObject with ARObjectController attached.
    /// </summary>
    public static GameObject Create(
        PlantCatalogEntry entry,
        Transform parent,
        ARPlacementConfig config,
        Camera arCamera,
        Vector3 prefabRotationOffsetEuler)
    {
        if (entry == null || entry.prefab == null) return null;

        GameObject obj = Object.Instantiate(entry.prefab, parent);
        obj.transform.localPosition = Vector3.zero;
        obj.transform.localRotation = Quaternion.identity;

        string[] ignoreNames = entry.ignoreRendererNames;

        // Physics layer isolation
        SetLayerRecursive(obj, config.potPhysicsLayer);

        // Uniform height normalization
        Vector3 ns = ARBoundsUtility.NormalizeHeight(obj, config.defaultPotHeightCm, ignoreNames);
        obj.transform.localScale = ns * Mathf.Max(config.defaultScale, 0.1f);

        // Face camera at placement time (local rotation relative to parent)
        if (arCamera != null && parent != null)
        {
            Vector3 dir = arCamera.transform.position - parent.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.001f)
            {
                Quaternion worldRot = Quaternion.LookRotation(-dir.normalized)
                    * Quaternion.Euler(prefabRotationOffsetEuler);
                obj.transform.localRotation =
                    Quaternion.Inverse(parent.rotation) * worldRot;
            }
        }

        ARBoundsUtility.FitCollider(obj, ignoreNames);
        ARBoundsUtility.Ground(obj, config.yOffset, ignoreNames);
        StripDisallowedComponents(obj);
        ForceOpaqueMaterials(obj);
        ForceRendererAlwaysVisible(obj);

        // Ensure top collider is a trigger
        var topCol = obj.GetComponent<BoxCollider>();
        if (topCol) topCol.isTrigger = true;

        // Attach controller + cache renderers
        var ctrl = obj.AddComponent<ARObjectController>();
        ctrl.CatalogEntry = entry;
        ctrl.NormalizedScale = ns;
        ctrl.SurfaceY = parent.position.y + config.yOffset;
        ctrl.CacheRenderers();

        return obj;
    }

    // ────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ────────────────────────────────────────────────────────────────

    public static void SetLayerRecursive(GameObject obj, int layer)
    {
        obj.layer = layer;
        foreach (Transform child in obj.transform)
            SetLayerRecursive(child.gameObject, layer);
    }

    /// <summary>
    /// Remove only explicitly disallowed component types that cause
    /// autonomous movement or conflict with the AR placement system.
    /// Does NOT blindly destroy all MonoBehaviours — unknown scripts
    /// (shaders, VFX controllers, custom renderers) are left intact.
    /// </summary>
    public static void StripDisallowedComponents(GameObject root)
    {
        // Physics components that cause movement
        foreach (var j in root.GetComponentsInChildren<Joint>(true))
            Object.Destroy(j);
        foreach (var rb in root.GetComponentsInChildren<Rigidbody>(true))
        {
            rb.isKinematic = true;
            rb.detectCollisions = false;
            Object.Destroy(rb);
        }
        foreach (var cl in root.GetComponentsInChildren<Cloth>(true))
            Object.Destroy(cl);
        foreach (var cc in root.GetComponentsInChildren<CharacterController>(true))
            Object.Destroy(cc);

        // Animation components (prevent autonomous transform changes)
        foreach (var a in root.GetComponentsInChildren<Animator>(true))
            Object.Destroy(a);
        foreach (var a in root.GetComponentsInChildren<Animation>(true))
            Object.Destroy(a);

        // Navigation
        foreach (var nav in root.GetComponentsInChildren<UnityEngine.AI.NavMeshAgent>(true))
            Object.Destroy(nav);
        foreach (var obs in root.GetComponentsInChildren<UnityEngine.AI.NavMeshObstacle>(true))
            Object.Destroy(obs);

        // Keep only top-level collider (ours, fitted by FitCollider)
        var allCols = root.GetComponentsInChildren<Collider>(true);
        foreach (var col in allCols)
        {
            if (col.gameObject == root) continue;
            Object.Destroy(col);
        }
    }

    public static void ForceOpaqueMaterials(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (!r) continue;
            foreach (var mat in r.materials)
            {
                if (!mat) continue;
                if (mat.HasProperty("_Surface"))
                {
                    mat.SetFloat("_Surface", 0f);
                    mat.SetFloat("_Blend", 0f);
                }
                if (mat.HasProperty("_Mode"))
                    mat.SetFloat("_Mode", 0f);
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
                mat.DisableKeyword("_ALPHAPREMULTIPLY_ON");
                mat.DisableKeyword("_ALPHATEST_ON");
                mat.DisableKeyword("_ALPHABLEND_ON");
                mat.DisableKeyword("_SURFACE_TYPE_TRANSPARENT");
                mat.renderQueue = 2000;
            }
        }
    }

    public static void ForceRendererAlwaysVisible(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (!r) continue;
            r.allowOcclusionWhenDynamic = false;
        }
    }
}
