import { lazy, newInstance } from "aurelia-framework"
import { HttpClient, json } from "aurelia-fetch-client"

const serviceBaseUrl = "http://localhost:8080/thesis.webapi/rest/";

export class Services {

  endpoints = {
    learner: "learner",
    question: "question"
  };

  constructor( @newInstance(HttpClient) public httpClient: HttpClient) {
    httpClient.configure(config => {
      config
        .withBaseUrl(serviceBaseUrl)
        .withDefaults({
          mode: 'cors',
          headers: {
            'Accept': 'application/json',
            'Content-type': 'application/json'
          }
        })
    });
  }

  get(url: string) {
    return this.makeRemoteCall('get', url);
  }

  post(url: string, data: any) {
    return this.makeRemoteCall('post', url, data);
  }

  private makeRemoteCall(methodName: string, url: string, data?: any) {
    return this.httpClient
      .fetch(url, {
        method: methodName,
        body: data ? json(data) : undefined
      })
      .then(response => response.json());
  }
}
