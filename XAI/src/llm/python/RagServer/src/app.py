from flask import Flask, request, jsonify
import os
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from sentence_transformers import SentenceTransformer
import faiss

from model import ChatModel
import rag_util

import os
os.environ["PYTORCH_CUDA_ALLOC_CONF"] = "expandable_segments:True"

import torch
torch.cuda.empty_cache()

app = Flask(__name__)

# Load environment variables
from dotenv import load_dotenv

load_dotenv()

# Directory paths
# FILES_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "files"))
CACHE_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models"))
# os.makedirs(FILES_DIR, exist_ok=True)


# Load models once and cache
model = ChatModel(model_id="google/gemma-2b-it", device="cuda")
encoder = rag_util.Encoder(
        model_name="sentence-transformers/all-MiniLM-L12-v2", device="cpu"
    )
files = []
DB = []
#
# # FAISS index for semantic search
# index = faiss.IndexFlatL2(encoder.get_sentence_embedding_dimension())

def save_file(uploaded_file):
    with open(uploaded_file, "wb") as f:
        f.write(uploaded_file.getbuffer())
    return uploaded_file

# Endpoint to upload files
@app.route('/upload', methods=['POST'])
def upload_files():
    global DB, files
    data = request.get_json()
    file_paths = data.get("file_paths", [])

    # Validate paths
    files = []
    for file_path in file_paths:
        if os.path.exists(file_path) and file_path.lower().endswith('.pdf'):
            files.append(file_path)
        else:
            return jsonify({"error": f"Invalid or non-existent file path: {file_path}"}), 400

    # for file in valid_file_paths:
    #     file_paths.append(save_file(file))
    # Load and split PDFs, build FAISS index
    print(files)
    if files != []:
        docs = rag_util.load_and_split_pdfs(files)
        DB = rag_util.FaissDb(docs=docs, embedding_function=encoder.embedding_function)

    return jsonify({"message": "Files uploaded and processed", "files": file_paths})


# Endpoint for generating responses
@app.route('/generate', methods=['POST'])
def generate_response():
    data = request.get_json()
    prompt = data.get("prompt")
    k = data.get("k", 3)
    max_new_tokens = data.get("max_new_tokens", 250)

    # Search for context if files are uploaded
    context = (
        None if files == [] else DB.similarity_search(prompt, k=k)
    )
    response = model.generate(
        prompt, context=context, max_new_tokens=max_new_tokens
    )
    print(f"User: {prompt}")
    print(f"Bot: {response}")
    return jsonify({"response": response, "context": context})


# Add a health check endpoint or homepage
@app.route('/', methods=['GET'])
@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "Server is running"}), 200

# Run the app
if __name__ == '__main__':
    app.run(debug=True, use_reloader=False)
