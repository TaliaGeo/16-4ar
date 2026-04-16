using UnityEngine;

/// <summary>
/// ScriptableObject defining a single plant/pot available for AR placement.
/// Create one asset per plant via Assets → Create → AR → Plant Catalog Entry.
/// </summary>
[CreateAssetMenu(fileName = "NewPlant", menuName = "AR/Plant Catalog Entry")]
public class PlantCatalogEntry : ScriptableObject
{
    [Tooltip("Display name shown in the menu.")]
    public string displayName = "Plant";

    [Tooltip("Prefab instantiated when this plant is placed.")]
    public GameObject prefab;

    [Tooltip("Real-world height of this plant in centimeters.")]
    public float realHeightCm = 30f;

    [Tooltip("Preview sprite shown in the plant selection menu.")]
    public Sprite menuSprite;

    [Tooltip("Renderer name substrings to ignore when computing bounds (e.g. shadow, ground).")]
    public string[] ignoreRendererNames = new string[] { "ground", "shadow", "plane" };
}
