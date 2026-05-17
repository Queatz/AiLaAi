const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');

config.optimization = {
    splitChunks: {
        chunks: 'all',
        minSize: 50000,
        maxSize: 1000000,
        cacheGroups: {
            babylon: {
                test: /[\\/]node_modules[\\/]@babylonjs[\\/]/,
                name: 'babylon',
                chunks: 'all',
                priority: 40,
            },
            monaco: {
                test: /[\\/]node_modules[\\/]monaco-editor[\\/]/,
                name: 'monaco',
                chunks: 'all',
                priority: 40,
            },
            mapbox: {
                test: /[\\/]node_modules[\\/]mapbox-gl[\\/]/,
                name: 'mapbox',
                chunks: 'all',
                priority: 40,
            },
            videosdk: {
                test: /[\\/]node_modules[\\/]@videosdk.live[\\/]/,
                name: 'videosdk',
                chunks: 'all',
                priority: 40,
            },
            kotlin_core: {
                test: /[\\/](kotlin-kotlin-stdlib|html-html-core|androidx-compose-runtime-runtime|kotlinx-coroutines-core|html-internal-html-core-runtime|compose-multiplatform-core-compose-runtime-runtime)(\.m?js|[\\/]|$)/,
                name: 'kotlin-core',
                chunks: 'all',
                priority: 30,
                enforce: true,
            },
            vendor: {
                test: /[\\/]node_modules[\\/]/,
                name: 'vendors',
                chunks: 'all',
                priority: 20,
            },
        },
    },
    runtimeChunk: 'single',
};

config.plugins.push(new HtmlWebpackPlugin({
    template: path.resolve(__dirname, 'kotlin/index.html'),
    inject: 'body',
    publicPath: '/'
}));

config.target = ['web'];
