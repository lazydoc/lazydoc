
<!DOCTYPE html>
<html>
<head>
    <title>Swagger UI</title>
    <link href='${pageContext.request.contextPath}/resources/css/reset.css' media='screen' rel='stylesheet' type='text/css'/>
    <link href='${pageContext.request.contextPath}/resources/css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
    <link href='${pageContext.request.contextPath}/resources/css/reset.css' media='print' rel='stylesheet' type='text/css'/>
    <link href='${pageContext.request.contextPath}/resources/css/screen.css' media='print' rel='stylesheet' type='text/css'/>
    <script src="${pageContext.request.contextPath}/resources/js/shred.bundle.js" type="text/javascript"></script>
    <script src='${pageContext.request.contextPath}/resources/js/jquery-1.8.0.min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/jquery.slideto.min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/jquery.wiggle.min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/jquery.ba-bbq.min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/handlebars-1.0.0.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/underscore-min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/backbone-min.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/swagger-ui.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/highlight.7.3.pack.js' type='text/javascript'></script>

    <script src='${pageContext.request.contextPath}/resources/js/swagger-client.js' type='text/javascript'></script>
    <script src='${pageContext.request.contextPath}/resources/js/swagger.js' type='text/javascript'></script>


    <script type="text/javascript">
        $(function () {
            window.swaggerUi = new SwaggerUi({
                url: "${pageContext.request.contextPath}/discover",
                dom_id: "swagger-ui-container",
                supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
                onComplete: function(swaggerApi, swaggerUi){
                    log("Loaded SwaggerUI");
                    $('pre code').each(function(i, e) {
                        hljs.highlightBlock(e)
                    });
                },
                onFailure: function(data) {
                    log("Unable to Load SwaggerUI");
                },
                docExpansion: "none",
                sorter : "alpha"
            });

            $('#input_apiKey').change(function() {
                var key = $('#input_apiKey')[0].value;
                log("key: " + key);
                if(key && key.trim() != "") {
                    log("added key " + key);
                    window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
                }
            })
            window.swaggerUi.load();
        });
    </script>
</head>

<body class="swagger-section">
<div id='header'>
    <div class="swagger-ui-wrap">
        <a id="logo" href="http://swagger.wordnik.com">swagger</a>
        <form id='api_selector'>
            <div class='input'><input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl" type="text"/></div>
            <div class='input'><input placeholder="api_key" id="input_apiKey" name="apiKey" type="text"/></div>
            <div class='input'><a id="explore" href="#">Explore</a></div>
        </form>
    </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">&nbsp;</div>
<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
</body>
</html>
