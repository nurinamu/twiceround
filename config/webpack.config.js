var webpack = require("webpack");

module.exports = [{
    entry : ['./output/twiceround.js','./js/main.js'],
    output : {
        filename: 'bundle.js',
        path: 'app/js'
    },
    plugins : [
        new webpack.ProvidePlugin({
            kotlin : './lib/kotlin.js',
            $ : 'jquery'
        })
    ]
}];
