// webpack.config.js

const path = require('path');

module.exports = {
    entry: {
        client: './src/client.ts',
        frame: './src/frame.ts',
    },
    devtool: 'source-map',
    mode: 'production',
    target: ['web', 'es5'],
    resolve: {
        extensions: ['.ts', '.js'],
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
        ],
    },
    output: {
        filename: '[name].js',
        path: __dirname + '/dist',
    },
};
