html: |
  $html$
attributes:
$attributes.keys:{key |   $key$: $attributes.(key)$}; separator="\n"$
frontMatter:
$frontMatter.keys:{key |   $key$: $frontMatter.(key)$}; separator="\n"$
metadata:
$metadata.keys:{key |   $key$: $metadata.(key)$}; separator="\n"$