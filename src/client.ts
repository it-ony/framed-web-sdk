import { Channel } from "./channel";
import { ProxyMethodCall } from "./frame";

type Region = 'EU' | 'US' | 'CA'

type BootstrapParameter = {
    token?: string,
    url?: string,
    clientId?: string,
    containerId?: string,
    containerEl?: Element,
    workflowLinkId?: string,
    region?: Region
} & Record<string, any>

type Listener = (event: object) => void

const sandboxPermissions = ["forms",
    "modals",
    "orientation-lock",
    "pointer-lock",
    "popups",
    "popups-to-escape-sandbox",
    "presentation",
    "same-origin",
    "scripts",
    "top-navigation"].map(x => `allow-${x}`);

const featurePermissions = [
    "camera",
    "microphone",
    "geolocation"
];

class EventBus {
    private handler: Record<string, Listener[]> = {}

    trigger(name: string, event: object) {
        const listeners = this.handler[name];
        if (listeners) {
            listeners.forEach((listener) => {
                listener(event)
            })
        }
    }

    addEventListener(name: string, listener: Listener): void {
        this.handler[name] = this.handler[name] || [];
        this.handler[name].push(listener)
    }

    // noinspection JSUnusedGlobalSymbols
    removeEventListener(name: string, listener: Listener): void {
        const listeners = this.handler[name];
        if (!listeners) {
            return
        }

        listeners.splice(listeners.indexOf(listener), 1)
    }


}


class PrivateHandle extends EventBus {

    private _initialized = false;
    private _initializationTimer?: number;
    private _tearedDown = false;

    private frame?: HTMLIFrameElement;
    private channel?: Channel;
    private readonly proxyParameters: Record<string, any>;
    private readonly proxy: Record<string, (...args: any[]) => void>;
    private tearDownHooks: (() => void)[] = [];

    private readonly _options: Record<string, any>;


    constructor(parameters: BootstrapParameter) {
        super()

        PrivateHandle.validateParameters(parameters);

        this._options = parameters;

        const [proxyParameters, proxy] = this.createProxyParameters(parameters)
        this.proxyParameters = proxyParameters;
        this.proxy = proxy;

        const mount = this.getMount(parameters);

        let url = (parameters.url || Onfido.FRAME_URL).replace("<region>", parameters.region!.toLowerCase());

        if (parameters.workflowLinkId) {
            url = `${url}/${parameters.workflowLinkId}`
        }

        this.frame = PrivateHandle.createFrame(url)

        mount.appendChild(this.frame);

        this.channel = new Channel(window, <WindowProxy>this.frame.contentWindow, {
            initialized: this.initialized,
            proxyCall: this.proxyCall,
            userAnalyticsEvent: this.userAnalyticsEvent,
            error: this.frameError
        })

        this.frame.addEventListener("load", this.loadHandler)
        this.frame.addEventListener("error", this.errorHandler)

    }


    public tearDown() {
        this.tearDownHooks.forEach(fnc => fnc())

        this.frame?.remove();

        this.tearDownHooks = [];
        this.frame = undefined;
        this.channel = undefined;

        this._tearedDown = true;
    }

    public setOptions() {
        this.verifyActive();

        // TODO: implement, setOptions
        throw new Error("Not implemented");
    }

    public get containerId(): string | undefined {
        return this.options.containerId;
    }

    public get options(): Record<string, any> {
        return this._options;
    }

    private verifyActive() {
        if (this._tearedDown) {
            throw new Error("`tearDown` was invoked. SDK is not interactive anymore.")
        }
    }

    private initialized = () => {
        this._initialized = true;
        this._initializationTimer && window.clearTimeout(this._initializationTimer);

        this.bootstrapSdk();
    }

    private loadHandler = () => {
        this.frame?.removeEventListener("load", this.loadHandler);

        if (!this._initialized) {
            this._initializationTimer = window.setTimeout(() => {
                this.trigger("error", new Error("Initialization took to long"));
            }, 1000);
        }
    };

    private errorHandler = (e: ErrorEvent) => {
        this.trigger("error", e);
        this.tearDown();
    }

    private frameError = (e:object) => {
        this.trigger("error", e);
        this.tearDown();
    }

    private bootstrapSdk() {
        this.channel?.call("bootstrap", this.proxyParameters)
    }

    private proxyCall = (prop: ProxyMethodCall) => {
        const proxyMethod = this.proxy[prop.proxyName];

        if (proxyMethod) {
            proxyMethod.apply(this, prop.arguments)
        } else {
            console.error(`proxy for method ${prop.proxyName} not registered.`)
        }
    }

    private userAnalyticsEvent = (details: any) => {
        window.dispatchEvent(new CustomEvent("userAnalyticsEvent", { detail: details }));
    }

    private static createFrame(url: string) {

        const frame: HTMLIFrameElement = document.createElement('iframe')
        frame.setAttribute("style", "width: 100%; min-height: 600px; height: 100%; border: 0");
        frame.allowFullscreen = true
        frame.setAttribute("sandbox", sandboxPermissions.join(" "));
        frame.allow = featurePermissions.map(x => `${x} *`).join(";")
        frame.src = url

        return frame
    }

    private getMount(parameters: BootstrapParameter): Element {

        if (parameters.containerEl) {
            return parameters.containerEl;
        } else if (parameters.containerId) {
            const element = document.getElementById(parameters.containerId);

            if (!element) {
                throw new Error(`Mount with id "${parameters.containerId}" not found.`)
            }

            return element;
        }

        const div = document.createElement("div");
        document.body.appendChild(div);

        this.tearDownHooks.push(() => {
            div.remove();
        })

        return div;
    }

    private createProxyParameters(parameters: BootstrapParameter): [Record<string, any>, Record<string, (...args: any[]) => void>] {

        const skipKeys = ["containerId", "containerEl", "workflowLinkId", "region"]

        const proxy: Record<string, (...args: any[]) => void> = {}

        // @ts-ignore
        const proxyParameters = Object.fromEntries(Object.entries(parameters).map(([key, value]) => {
            if (skipKeys.includes(key)) {
                return undefined;
            }

            if (typeof value === 'function') {
                const proxyName = `__proxy_${key}`;
                proxy[proxyName] = value;
                return [key, proxyName];
            }

            return [key, value]
        }).filter(x => x !== undefined))

        return [proxyParameters, proxy]

    }

    private static validateParameters(parameters: BootstrapParameter) {

        const token = `'token'`;
        const workflowLinkId = `'workflowLinkId'`;

        if (!parameters.token && !parameters.workflowLinkId) {
            throw new Error(`${token} or ${workflowLinkId} needs to be present.`)
        }

        if (parameters.token && parameters.workflowLinkId) {
            throw new Error(`Only one of [${token}, ${workflowLinkId}] can be provided.`)
        }

        if (parameters.workflowLinkId && !parameters.region) {
            throw new Error(`${workflowLinkId} required to provide the 'region' as well.`)
        }

    }
}


export type Handle = {
    tearDown: () => void,
    addEventListener: (name: string, listener: Listener) => void,
    removeEventListener: (name: string, listener: Listener) => void
}

export class Onfido {

    static FRAME_URL: string = "https://sdk.<region>.onfido.app/frame"

    init(parameters: BootstrapParameter): Handle {
        const handle = new PrivateHandle(parameters);

        return {
            tearDown: () => {
                return handle.tearDown()
            },
            addEventListener: (name: string, listener: Listener) => {
                return handle.addEventListener(name, listener);

            },
            removeEventListener: (name: string, listener: Listener) => {
                return handle.removeEventListener(name, listener)
            }
        };
    }

}


// @ts-ignore
window['Onfido'] = new Onfido()
