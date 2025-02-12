import base64
from io import BytesIO

import numpy as np
from PIL import Image
import os
import torch
import datetime

from torch import nn
from torchvision import transforms, utils

#todo: make it auto encoder to use latent space
class StandardUNet(nn.Module):
    def __init__(self, device):
        super().__init__()

        self.device = device

        self.f1 = create_conv_block(3, 64)
        self.p1 = nn.MaxPool2d(kernel_size=2, stride=2)

        self.f2 = create_conv_block(64, 128)
        self.p2 = nn.MaxPool2d(kernel_size=2, stride=2)

        self.f3 = create_conv_block(128, 256)
        self.p3 = nn.MaxPool2d(kernel_size=2, stride=2)

        self.f4 = create_conv_block(256, 512)
        self.p4 = nn.MaxPool2d(kernel_size=2, stride=2)

        self.f5 = create_conv_block(512, 1024)
        self.p5 = nn.ConvTranspose2d(1024, 512, kernel_size=3, stride=2)

        self.f6 = create_conv_block(1024, 512)
        self.p6 = nn.ConvTranspose2d(512, 256, kernel_size=3, stride=2)

        self.f7 = create_conv_block(512, 256)
        self.p7 = nn.ConvTranspose2d(256, 128, kernel_size=2, stride=2)

        self.f8 = create_conv_block(256, 128)
        self.p8 = nn.ConvTranspose2d(128, 64, kernel_size=2, stride=2)

        self.f9 = create_conv_block(128, 64)
        self.p9 = nn.Conv2d(64, 3, kernel_size=1)

    def forward(self, x):
        # x = x.to(self.device)

        f1 = self.f1(x)
        p1 = self.p1(f1)

        f2 = self.f2(p1)
        p2 = self.p2(f2)

        f3 = self.f3(p2)
        p3 = self.p3(f3)

        f4 = self.f4(p3)
        p4 = self.p4(f4)

        f5 = self.f5(p4)
        p5 = self.p5(f5)

        f6 = self.f6(torch.cat([f4, p5], dim=1))
        p6 = self.p6(f6)

        f7 = self.f7(torch.cat([f3, p6], dim=1))
        p7 = self.p7(f7)

        f8 = self.f8(torch.cat([f2, p7], dim=1))
        p8 = self.p8(f8)

        f9 = self.f9(torch.cat([f1, p8], dim=1))
        p9 = self.p9(f9)

        return p9

    def get_features(self, x):
        x = x.to(self.device)

        f1 = self.f1(x)
        p1 = self.p1(f1)

        f2 = self.f2(p1)
        p2 = self.p2(f2)

        f3 = self.f3(p2)
        p3 = self.p3(f3)

        f4 = self.f4(p3)
        p4 = self.p4(f4)

        f5 = self.f5(p4)

        return f5

def create_conv_block(num_channels_in, num_channels_out):
    block = nn.Sequential(
        nn.Conv2d(num_channels_in, num_channels_out, kernel_size=3, padding=1),
        nn.ReLU(),
        nn.Conv2d(num_channels_out, num_channels_out, kernel_size=3, padding=1),
        nn.ReLU()
    )

    return block



device = ("cuda" if torch.cuda.is_available()
              else "cpu")
unet = StandardUNet(device).to(device)
PATH = ("C:\\Users\\adrie\\Documents\\DACS\\Internship\\Ludii-XAI\\XAI\\src\\llm\\python\\RagServer\\models\\outline_model.pt")
unet.load_state_dict(torch.load(PATH, weights_only=True))


def get_features(sample):
    # Convert the Base64 string to an image
    image_data = base64.b64decode(sample)
    image = Image.open(BytesIO(image_data)).convert("RGB")

    # Convert the image to a PyTorch tensor
    # tensor = torch.tensor(np.array(image), dtype=torch.float32).permute(2, 0, 1)  # (H, W, C) -> (C, H, W)
    #
    # # Normalize & reshape tensor to match the expected input (batch, channels, height, width)
    # tensor = tensor / 255.0  # Normalize to [0,1]
    # tensor = tensor.unsqueeze(0)  # Add batch dimension
    #
    # # Move to the correct device
    # tensor = tensor.to(unet.device)

    transform = transforms.ToTensor()
    tensor = transform(image).unsqueeze(0)
    tensor = tensor.to(device)

    # Extract features using UNet
    features = unet.get_features(tensor)
    print(np.shape(features.detach().cpu().numpy()))
    # Convert tensor to a list for JSON serialization
    return features.detach().cpu().numpy().tolist()