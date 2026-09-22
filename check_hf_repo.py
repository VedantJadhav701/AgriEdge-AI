from huggingface_hub import HfApi

api = HfApi()
try:
    files = api.list_repo_files(repo_id="vedantjadhav701/agriedge-classifier")
    print("Files in vedantjadhav701/agriedge-classifier:")
    for f in files:
        print("  -", f)
except Exception as e:
    print("Error listing repo files:", e)
