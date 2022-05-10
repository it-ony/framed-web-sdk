# Framed Web SDK

Onfido offers the [web SDK](https://github.com/onfido/onfido-sdk-ui) as an open source project on github. The web sdk
is a library that can be bootstrapped with a set of parameters and renders a user interface to capture document and face 
information from an applicant.

It's running in the context of a 3rd party and therefore updating to the latest version is out of control of Onfido. The 
framed Web SDK is an idea to host a small javascript that bootstraps the web sdk in an iframe. The iframe will be hosted 
by onfido, so we can deliver the latest version of the SDK to the customers without any change on their end.

# How to use it

