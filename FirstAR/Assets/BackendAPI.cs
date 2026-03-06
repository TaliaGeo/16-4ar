using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

public class BackendAPI : MonoBehaviour
{
    private const string BaseUrl = "http://localhost:8081/api";

    void Start()
    {
        StartCoroutine(GetRequest(BaseUrl));
    }

    IEnumerator GetRequest(string url)
    {
        using (UnityWebRequest request = UnityWebRequest.Get(url))
        {
            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.ConnectionError ||
                request.result == UnityWebRequest.Result.ProtocolError)
            {
                Debug.LogError("Backend connection error: " + request.error);
            }
            else
            {
                Debug.Log("Backend response: " + request.downloadHandler.text);
            }
        }
    }
}