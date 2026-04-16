using System;
using System.Linq;
using UnityEditor;
using UnityEditor.Build.Reporting;

public static class CommandLineBuild
{
    public static void BuildAndroid()
    {
        string outputPath = Environment.GetEnvironmentVariable("UNITY_ANDROID_OUTPUT_APK");
        if (string.IsNullOrWhiteSpace(outputPath))
        {
            outputPath = "FirstAR_CLI_Build.apk";
        }

        string[] scenes = EditorBuildSettings.scenes
            .Where(s => s.enabled)
            .Select(s => s.path)
            .ToArray();

        if (scenes.Length == 0)
        {
            throw new Exception("No enabled scenes found in EditorBuildSettings.");
        }

        var buildPlayerOptions = new BuildPlayerOptions
        {
            scenes = scenes,
            locationPathName = outputPath,
            target = BuildTarget.Android,
            options = BuildOptions.None
        };

        BuildReport report = BuildPipeline.BuildPlayer(buildPlayerOptions);
        BuildSummary summary = report.summary;

        if (summary.result != BuildResult.Succeeded)
        {
            throw new Exception("Unity Android build failed: " + summary.result);
        }

        UnityEngine.Debug.Log("Android build succeeded: " + summary.outputPath);
    }
}
