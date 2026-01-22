# Re-index Project

Re-index the inside-health-hms project using LEANN for semantic code search.

Run the following command:

```bash
leann build inside-health-hms --docs $(git ls-files) --embedding-mode sentence-transformers --embedding-model all-MiniLM-L6-v2 --backend hnsw --force
```

This will rebuild the search index with all tracked files in the repository.
