#!/bin/bash
#
# List all available functions in the shared library
#

echo "======================================================"
echo "Irontec Jenkins Shared Library - Available Functions"
echo "======================================================"
echo ""

cd "$(dirname "$0")/vars" || exit 1

for file in *.groovy; do
    function_name="${file%.groovy}"

    # Extract the first line of documentation
    description=$(grep -A 1 "^/\*\*" "$file" |
        grep -v "^/\*\*" |
        grep -v "^--" |
        sed 's/^ \* //' |
        sed 's/^[ \t]*//' |
        head -1)

    printf "%-35s %s\n" "$function_name" "$description"
done | sort

echo ""
echo "======================================================"
echo "Total: $(ls -1 *.groovy | wc -l) functions"
echo ""
echo "For detailed documentation, see README.md"
echo "======================================================"

