using UnityEngine;

/// <summary>
/// Static utility for bounds computation, footprint-based overlap checks,
/// grounding, and height normalization. No MonoBehaviour — pure static helpers.
/// </summary>
public static class ARBoundsUtility
{
    /// <summary>World-space AABB encapsulating all non-ignored renderers.</summary>
    public static Bounds WorldBounds(GameObject root, string[] ignoreNames)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Renderer first = null;
        foreach (var r in rs)
            if (!ShouldIgnore(r, ignoreNames)) { first = r; break; }
        if (!first) return new Bounds(root.transform.position, Vector3.zero);

        Bounds b = first.bounds;
        foreach (var r in rs)
            if (!ShouldIgnore(r, ignoreNames)) b.Encapsulate(r.bounds);
        return b;
    }

    /// <summary>Local-space bounds relative to root's transform.</summary>
    public static Bounds LocalBounds(GameObject root, string[] ignoreNames)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Matrix4x4 w2l = root.transform.worldToLocalMatrix;
        Bounds lb = new Bounds(Vector3.zero, Vector3.zero);
        bool first = true;

        foreach (var r in rs)
        {
            if (ShouldIgnore(r, ignoreNames)) continue;
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

    /// <summary>Normalize object scale so its world height equals the target centimeters.</summary>
    public static Vector3 NormalizeHeight(GameObject root, float cm, string[] ignoreNames)
    {
        Bounds b = WorldBounds(root, ignoreNames);
        float f = (cm / 100f) / Mathf.Max(0.0001f, b.size.y);
        root.transform.localScale = Vector3.one * f;
        return root.transform.localScale;
    }

    /// <summary>
    /// Ground the object so its visual bottom sits on the anchor's Y plane.
    /// </summary>
    public static void Ground(GameObject obj, float yOffset, string[] ignoreNames)
    {
        Bounds lb = LocalBounds(obj, ignoreNames);
        if (lb.size == Vector3.zero) return;

        float scaleY = obj.transform.localScale.y;
        Vector3 lp = obj.transform.localPosition;
        lp.y = yOffset - lb.min.y * scaleY;
        obj.transform.localPosition = lp;

        // Verify with world bounds
        Bounds wb = WorldBounds(obj, ignoreNames);
        if (wb.size != Vector3.zero && obj.transform.parent != null)
        {
            float surfaceY = obj.transform.parent.position.y + yOffset;
            float gap = wb.min.y - surfaceY;
            if (Mathf.Abs(gap) > 0.001f)
            {
                lp = obj.transform.localPosition;
                lp.y -= gap;
                obj.transform.localPosition = lp;
            }
        }
    }

    /// <summary>Fit a trigger BoxCollider tightly around the object's world bounds.</summary>
    public static void FitCollider(GameObject root, string[] ignoreNames)
    {
        Bounds w = WorldBounds(root, ignoreNames);
        if (w.size == Vector3.zero) return;

        BoxCollider bc = root.GetComponent<BoxCollider>() ?? root.AddComponent<BoxCollider>();
        bc.center = root.transform.InverseTransformPoint(w.center);
        Vector3 ls = root.transform.lossyScale;
        bc.size = new Vector3(
            ls.x != 0 ? w.size.x / ls.x : w.size.x,
            ls.y != 0 ? w.size.y / ls.y : w.size.y,
            ls.z != 0 ? w.size.z / ls.z : w.size.z);
        bc.isTrigger = true;
    }

    /// <summary>
    /// Compute the real base footprint of a grounded object: the world-space
    /// XZ extent at the bottom of the object, with a thin Y slab. This uses
    /// the actual collider if present, otherwise falls back to renderer bounds.
    /// Far more accurate than full-height AABB for pot-shaped objects.
    /// </summary>
    public static Bounds BaseFootprint(GameObject root, string[] ignoreNames)
    {
        // Try using BoxCollider first — it was fitted to the real mesh
        BoxCollider bc = root.GetComponent<BoxCollider>();
        if (bc != null)
        {
            Bounds colWorld = bc.bounds;
            // Take the bottom 20% of the collider as the base footprint
            float baseHeight = colWorld.size.y * 0.2f;
            Vector3 fpCenter = new Vector3(colWorld.center.x, colWorld.min.y + baseHeight * 0.5f, colWorld.center.z);
            Vector3 fpSize = new Vector3(colWorld.size.x, baseHeight, colWorld.size.z);
            return new Bounds(fpCenter, fpSize);
        }

        // Fallback: renderer bounds bottom slice
        Bounds wb = WorldBounds(root, ignoreNames);
        if (wb.size == Vector3.zero) return wb;

        float slabH = wb.size.y * 0.2f;
        Vector3 center = new Vector3(wb.center.x, wb.min.y + slabH * 0.5f, wb.center.z);
        Vector3 size = new Vector3(wb.size.x, slabH, wb.size.z);
        return new Bounds(center, size);
    }

    /// <summary>
    /// Check if two base footprints overlap in XZ with a minimum gap.
    /// Uses 2D AABB intersection on the XZ plane only — Y is ignored
    /// since both objects are grounded on the same surface.
    /// </summary>
    public static bool FootprintsOverlap(Bounds a, Bounds b, float minGap)
    {
        float aMinX = a.min.x - minGap, aMaxX = a.max.x + minGap;
        float aMinZ = a.min.z - minGap, aMaxZ = a.max.z + minGap;

        float bMinX = b.min.x, bMaxX = b.max.x;
        float bMinZ = b.min.z, bMaxZ = b.max.z;

        return aMinX < bMaxX && aMaxX > bMinX
            && aMinZ < bMaxZ && aMaxZ > bMinZ;
    }

    private static bool ShouldIgnore(Renderer r, string[] ignoreNames)
    {
        if (!r) return true;
        if (ignoreNames == null) return false;
        string n = r.gameObject.name.ToLowerInvariant();
        foreach (var s in ignoreNames)
            if (!string.IsNullOrEmpty(s) && n.Contains(s.ToLowerInvariant()))
                return true;
        return false;
    }
}
