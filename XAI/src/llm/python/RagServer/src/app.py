from flask import Flask, request, jsonify
import os
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from sentence_transformers import SentenceTransformer
# import faiss

from model import ChatModel
import rag_util
# import sys
# app.logger.info(sys.executable)
import pandas as pd
import numpy as np
import ast

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
# CACHE_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models"))
# os.makedirs(FILES_DIR, exist_ok=True)


# Load models once and cache
model = ChatModel(model_id="google/gemma-2b-it", device="cuda")
encoder = rag_util.Encoder(
    model_name="sentence-transformers/all-MiniLM-L12-v2", device="cpu"
)

MAX_DEPTH, MAX_HEIGHT, MAX_WIDTH = 9, 15, 15
input_size = MAX_DEPTH * MAX_HEIGHT * MAX_WIDTH

# Define the Autoencoder class for 3D data
class AE(torch.nn.Module):
    def __init__(self, input_size):
        super().__init__()

        # Encoder: Reduce the dimensionality
        self.encoder = torch.nn.Sequential(
            torch.nn.Linear(input_size, 512),
            torch.nn.ReLU(),
            torch.nn.Linear(512, 256),
            torch.nn.ReLU(),
            torch.nn.Linear(256, 128),
            torch.nn.ReLU(),
            torch.nn.Linear(128, 64)
        )

        # Decoder: Reconstruct the original data
        self.decoder = torch.nn.Sequential(
            torch.nn.Linear(64, 128),
            torch.nn.ReLU(),
            torch.nn.Linear(128, 256),
            torch.nn.ReLU(),
            torch.nn.Linear(256, 512),
            torch.nn.ReLU(),
            torch.nn.Linear(512, input_size),
            torch.nn.Sigmoid()
        )

    def forward(self, x):
        encoded = self.encoder(x)
        decoded = self.decoder(encoded)
        return decoded

    def encode(self, x):
        return self.encoder(x)

board_distribution_autoencoder = AE(input_size)
board_distribution_autoencoder.load_state_dict(torch.load("C:\\Users\\adrie\\Documents\\DACS\\Internship\\Ludii-XAI\\XAI\\src\\llm\\python\\RagServer\\models\\autoencoder.pth"))

# Load the CSV file
df = pd.read_csv("C:\\Users\\adrie\\Documents\\DACS\\Internship\\Ludii-XAI\\XAI\\src\\llm\\python\\RagServer/data/features.csv")

# files = []
# DB = []
#
# # FAISS index for semantic search
# index = faiss.IndexFlatL2(encoder.get_sentence_embedding_dimension())

def save_file(uploaded_file):
    with open(uploaded_file, "wb") as f:
        f.write(uploaded_file.getbuffer())
    return uploaded_file

# Endpoint to upload files
# @app.route('/upload', methods=['POST'])
# def upload_files():
#     global DB, files
#     data = request.get_json()
#     file_paths = data.get("file_paths", [])
#
#     # Validate paths
#     files = []
#     for file_path in file_paths:
#         if os.path.exists(file_path) and file_path.lower().endswith('.pdf'):
#             files.append(file_path)
#         else:
#             return jsonify({"error": f"Invalid or non-existent file path: {file_path}"}), 400
#
#     # for file in valid_file_paths:
#     #     file_paths.append(save_file(file))
#     # Load and split PDFs, build FAISS index
#     app.logger.info(files)
#     if files != []:
#         docs = rag_util.load_and_split_pdfs(files)
#         DB = rag_util.FaissDb(docs=docs, embedding_function=encoder.embedding_function)
#
#     return jsonify({"message": "Files uploaded and processed", "files": file_paths})


def pad_or_crop(board):
    # Convert to a list to use append
    board = list(board)

    # Ensure consistent depth by adding planes at the start if necessary
    while len(board) < MAX_DEPTH:
        # Add zero planes at the start
        board.insert(0, np.zeros((len(board[0]), len(board[0][0]))))

    # Crop excess planes if necessary
    board = np.array(board[:MAX_DEPTH])

    # Pad or crop each plane
    padded_board = []
    for plane in board:
        # Pad at the top (before rows) and left (before columns)
        padded_plane = np.pad(
            plane,
            ((MAX_HEIGHT - plane.shape[0], 0), (0, MAX_WIDTH - plane.shape[1])),
            mode='constant',
            constant_values=0
        )
        padded_plane = padded_plane[:MAX_HEIGHT, :MAX_WIDTH]  # Crop if needed
        padded_board.append(padded_plane)

    return np.array(padded_board)


def compute_distance(row, features):
    """
    Compute the overall distance based on individual feature distances.
    """
    multiplier_colors = 1
    multiplier_boardDistribution = 1
    multiplier_removedColumns = 1
    multiplier_scoreOffset = 1
    multiplier_clusters = 1

    distances = []

    if "colors" in features:
        distances.append(multiplier_colors * abs(len(set(row["colors"].split(",")) - set(features["colors"]))))  # Set difference

    if "removedColumns" in features:
        distances.append(multiplier_removedColumns * abs(row["removedColumns"]-features["removedColumns"]))

    if "scoreOffset" in features:
        distances.append(multiplier_scoreOffset * abs(row["scoreOffset"]-features["scoreOffset"]))

    if "clusters" in features:
        distances.append(multiplier_clusters * abs(1))

    if "boardDistribution" in features:
        board = row["boardDistribution"]  # Safely parse string representation
        # Convert string representation of list to actual list
        if isinstance(board, str):
            board = ast.literal_eval(board)  # Safe conversion from string to list

        board = np.array(board, dtype=np.float32)  # Ensure the data is float32 for PyTorch compatibility
        board = pad_or_crop(board)  # Pad or crop to consistent size
        # Flatten the 3D array to a 1D tensor for autoencoder input
        board_flat = torch.tensor(board, dtype=torch.float32).view(-1)
        # Encode the board using the autoencoder
        encoded_board = board_distribution_autoencoder.encode(board_flat)

        feature = row["boardDistribution"]  # Safely parse string representation
        if isinstance(feature, str):
            feature = ast.literal_eval(feature)  # Safe conversion from string to list
        feature = np.array(feature, dtype=np.float32)  # Ensure the data is float32 for PyTorch compatibility
        feature = pad_or_crop(feature)  # Pad or crop to consistent size
        # Flatten the 3D array to a 1D tensor for autoencoder input
        feature_flat = torch.tensor(feature, dtype=torch.float32).view(-1)
        # Encode the board using the autoencoder
        encoded_feature = board_distribution_autoencoder.encode(feature_flat)

        distances.append(multiplier_boardDistribution * np.sum(np.abs(encoded_board.detach().numpy() - encoded_feature.detach().numpy())))  # Euclidean

    return sum(distances)  # You can also use a weighted sum


def similarFeatures(features, k=3):
    """
    Find the k most similar rows based on the computed distance.
    """
    df["distance"] = df.apply(lambda row: compute_distance(row, features), axis=1)

    # Get the top-k closest rows
    closest_explanations = df.nsmallest(k, "distance")["Explanation"].tolist()
    return closest_explanations


@app.route('/generate', methods=['POST'])
def generate_response():
    app.logger.info("Generating response...")
    data = request.get_json()
    prompt = data.get("prompt")
    k = data.get("k", 3)
    max_new_tokens = data.get("max_new_tokens", 250)
    features = data.get("features")

    # Search for context if files are uploaded
    context = (
        None if df.empty else similarFeatures(features, k=k)#DB.similarity_search(prompt, k=k)
    )
    response = model.generate(
        prompt, context=context, max_new_tokens=max_new_tokens
    )
    app.logger.info(f"User: {prompt}")
    app.logger.info(f"Bot: Provided context: {context} \nResponse: {response}")
    output = jsonify({"response": response, "context": context})
    app.logger.info(f"Output: {output}")
    return output


# Add a health check endpoint or homepage
@app.route('/', methods=['GET'])
@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "Server is running"}), 200

# Run the app
if __name__ == '__main__':
    app.run(debug=True, use_reloader=False)
