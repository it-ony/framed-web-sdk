# Framed Web SDK

Onfido offers the [web SDK](https://github.com/onfido/onfido-sdk-ui) as an open source project on github. The web sdk
is a library that can be bootstrapped with a set of parameters and renders a user interface to capture document and face 
information from an applicant.

It's running in the context of a 3rd party and therefore updating to the latest version is out of control of Onfido. The 
framed Web SDK is an idea to host a small javascript that bootstraps the web sdk in an iframe. The iframe will be hosted 
by onfido, so we can deliver the latest version of the SDK to the customers without any change on their end.

# How to use it
## Embedding and initialization

1. Embed the client javascript, `<script src="https://assets.onfido.com/web-sdk-client/client.js"></script>` in your html page. Do not deliver this javascript by
yourself, as we ensure it's always up to date and delivered fast via a cdn.
2. call `const handle = Onfido.init(parameter)` with an object. All parameters documented [here](https://github.com/onfido/onfido-sdk-ui#6-initialize-the-sdk) are supported.

## custom css

As we run the sdk within an iframe to secure your applicants most, you can style the content in the iframe to your needs by passing custom css via
the `css` parameter.

## tearing down

To tear down the sdk, use the handle object returned from the `Onfido.init` method and call `handle.tearDown()` to remove the SDK from your webpage.

# Modes of operation
## static
         
This is the old way of the web sdk, where the parameters and steps are pre-assigned and there is no workflow evaluated 
behind the scenes. Use this mode, if you do not have access to studio.

For this mode at least a [SDK token](https://github.com/onfido/onfido-sdk-ui#3-generate-an-sdk-token) needs to be passed 
as `token` in the parameter object.

```js
window.handle = Onfido.init({
    token: "<YOUR SDK TOKEN>" // https://github.com/onfido/onfido-sdk-ui#3-generate-an-sdk-token
});
```
                                                                                                
## workflow

With orchestration onfido offers a more dynamic and flexible way of sending the user through the required steps. The best
way of using orchestration is to make use out of workflow_links.

Therefore create a workflow link via the api first

```http request
POST https://api.<region>.onfido.com/v4/workflow_links
Content-Type: application/json
Authorization: Token token=<YOUR API KEY>

{
    "workflow_id": "<WORKFLOW_ID>",
}
```

Then use the id of the response payload

```json
{
    "id": "<ID>",
    "applicant_id": "<APPLICANT_ID>",
    "expires": "2022-06-26T12:56:25.547952",
    "url": "https://studio.eu.onfido.app/l/<ID>",
    "workflow_id": "<WORKFLOW_ID>"
}
```

to bootstrap the sdk with the following javascript code

```js
window.handle = Onfido.init({
    workflowLinkId: "<ID>"
});
```

# Example
     
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <script src="https://assets.onfido.com/web-sdk-client/client.js"></script>
    <script>
        window.handle = Onfido.init({
            token: "<YOUR SDK TOKEN>",
            onComplete: (outcome) => {
                console.log("complete", outcome)
            },
            
            steps: ["welcome", "face", "complete"],

            css: `
            html {
                background: repeating-linear-gradient(45deg, #606dbc, #606dbc 10px, #465298 10px, #465298 20px);
            }
            `
        })
    </script>
</body>
</html>
```




