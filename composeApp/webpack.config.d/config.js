config.devServer = {
    allowedHosts: "all",
    ...(config.devServer || {})
};