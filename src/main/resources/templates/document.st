<!DOCTYPE html>
<html lang="$if(attributes.lang)$$attributes.lang$$else$en$endif$">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$if(metadata.title)$$metadata.title$$else$Untitled$endif$</title>
    $frontMatter.keys:{key | <meta name="$key$" content="$frontMatter.(key)$">}; separator="\n    "$
    $if(attributes.stylesheet)$
    <link rel="stylesheet" href="$attributes.stylesheet$">
    $endif$
</head>
<body>
    <div class="container">
        $html$
    </div>
    $if(attributes.docinfo)$
    <!-- Additional document info -->
    <div class="docinfo">
        <p>Generated from: $metadata.file$</p>
    </div>
    $endif$
</body>
</html>