export type Invocation = {
    method: string,
    arguments: []
}

export class Channel {

    private readonly send: WindowProxy;
    private readonly handler: Record<string, () => void>;

    constructor(receive: Window, send: WindowProxy, handler: Record<string, any> = {}) {
        this.send = send;
        this.handler = handler;

        receive.addEventListener("message", this.handleMessage)
    }

    private handleMessage = (e: MessageEvent) => {

        if (e.source !== this.send) {
            // only accept messages from the channel
            return;
        }

        const invoke: Invocation = JSON.parse(e.data)
        const method = this.handler[invoke.method];

        if (!method) {
            return;
        }

        method.apply(this, invoke.arguments)

    }

    call(method: string, ...args: any[]) {
        this.send.postMessage(JSON.stringify({
            method: method,
            arguments: args
        }), "*")
    }
}
