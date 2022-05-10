# Framed Web SDK

Onfido offers the [web SDK](https://github.com/onfido/onfido-sdk-ui) as an open source project on github. The web sdk
is a library that can be bootstrapped with a set of parameters and renders a user interface to capture document and face 
information from an applicant.

It's running in the context of a 3rd party and therefore updating to the latest version is out of control of Onfido. The 
framed Web SDK is an idea to host a small javascript that bootstraps the web sdk in an iframe. The iframe will be hosted 
by onfido, so we can deliver the latest version of the SDK to the customers without any change on their end.

# How to use it
## Embedding and initialization

1. Embed the client javascript, `<script src="https://it-ony.github.io/framed-web-sdk/client.js"></script>` in your html page. Do not deliver this javascript by
yourself, as we ensure it's always up to date and delivered fast via a cdn.
2. call `const handle = Onfido.init(parameter)` with an object. All parameters documented [here](https://github.com/onfido/onfido-sdk-ui#6-initialize-the-sdk) are supported.
   
At least a [SDK token](https://github.com/onfido/onfido-sdk-ui#3-generate-an-sdk-token) needs to be passed as `token` in the parameter object.

## Additional parameters

### client slug, clientId

* `clientId` - you can pass the clientId, to get theming support. Go to the Dashboard to control the look and feel of the SDK.
* `clientSlug` - if you have setup a subdomain, you can also use this instead of the clientId to keep your clientId private

### custom css

As we run the sdk within an iframe to secure your applicants most, you can style the content in the iframe to your needs by passing 
custom css via the `css` parameter. 

## tearing down

To tear down the sdk, use the handle object returned from the `Onfido.init` method and call `handle.tearDown()` to remove the SDK from 
your webpage.


# Example
     
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <script src="https://it-ony.github.io/framed-web-sdk/client.js"></script>
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




