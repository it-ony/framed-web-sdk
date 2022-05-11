import { Channel } from "./channel";

type Sdk = {
    version: string,
    error?: object,
    parameter?: object
}

type SdkParameter = Record<string, any>
type OnfidoSdkHandle = {
    tearDown: () => void
}

type OnfidoSdk = {
    init: (parameter: SdkParameter) => OnfidoSdkHandle
}

type W = {
    sdk: Sdk,
    Onfido: OnfidoSdk
} & Window

export type ProxyMethodCall = {
    proxyName: string,
    arguments: any[]
}

const parameterHandler: Record<string, (key: string, value: any) => void | [string, any]> = {
    css: (key, value) => {
        const element = document.createElement('style');
        if ('textContent' in element) {
            element.textContent = value;
        } else {
            // @ts-ignore
            element.styleSheet.cssText = value;
        }

        document.head.appendChild(element)
    }
};

const filterParameterKeys:string[] = ['containerId', 'containerEl'];

((window: W, document: Document) => {

    const sdk: Sdk = window.sdk;

    const basePath = `https://assets.onfido.com/web-sdk-releases/${sdk.version}`

    loadCss(`${basePath}/style.css`);

    const sdkLoader = loadScript(`${basePath}/onfido.min.js`)
    const channel = new Channel(window, window.parent, {
        bootstrap: async (proxyParameter: Record<string, any>) => {

            const mapped: any[] = Object.entries(proxyParameter)
                .filter(([key]) => {
                    return filterParameterKeys.indexOf(key) === -1
                })
                .map(([key, value]) => {
                    if (typeof value === 'string' && value.startsWith("__proxy_")) {
                        // rewrite proxy methods
                        return [key, (...a: any[]) => {
                            const args: ProxyMethodCall = {
                                proxyName: value,
                                arguments: a
                            };
                            channel.call("proxyCall", args)
                        }]
                    } else if (key in parameterHandler) {
                        // unsupported parameters by the sdk, handled by the frame
                        return parameterHandler[key](key, value);
                    }

                    return [key, value]
                })
                .filter(x => x !== undefined)

            const parameter = Object.assign({}, Object.fromEntries(mapped), {
                // properties to always overwrite
                containerEl: document.body
            })

            await sdkLoader;

            document.querySelector("#spinner")?.remove()
            window.Onfido.init(parameter)
        }
    })

    if (sdk.error) {
        channel.call("error", sdk.error)
    } else {
        channel.call("initialized");
    }

    function loadCss(href: string) {
        const link = document.createElement("link");
        link.rel = "stylesheet";
        link.href = href;

        document.head.appendChild(link);
    }

    async function loadScript(src: string): Promise<void> {
        return new Promise((resolve, rejects) => {
            const script = document.createElement("script");
            script.src = src;

            script.onload = () => {
                resolve()
            }

            script.onerror = (e) => {
                rejects(e);
            }

            document.head.appendChild(script);

        })
    }

    window.addEventListener('userAnalyticsEvent', (event) => {
        // @ts-ignore
        channel.call("userAnalyticsEvent", event['detail'])
    });

})((window as unknown) as W, document)
