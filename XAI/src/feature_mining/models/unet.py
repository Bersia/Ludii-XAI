import os
import torch
import datetime

from torch import nn
from pathlib import Path
from torch.utils.tensorboard import SummaryWriter


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


class NoPoolUNet(nn.Module):
    def __init__(self, device):
        super().__init__()

        self.device = device

        self.f1 = create_conv_block(3, 64)
        self.f2 = create_conv_block(64, 128)
        self.f3 = create_conv_block(128, 256)
        self.f4 = create_conv_block(256, 512)

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
        x = x.to(self.device)

        f1 = self.f1(x)
        f2 = self.f2(f1)
        f3 = self.f3(f2)
        f4 = self.f4(f3)

        f5 = self.f5(f4)
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
        f2 = self.f2(f1)
        f3 = self.f3(f2)
        f4 = self.f4(f3)

        f5 = self.f5(f4)

        return f5


class ScaledUNet(nn.Module):
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

        self.scale = nn.Sigmoid()

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

        scale = self.scale(p9)

        return scale

    def get_features(self, x):
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


# https://pytorch.org/tutorials/beginner/introyt/trainingyt.html
def train_model(model, train_loader, val_loader, epochs, optimizer, loss_fn):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    writer = SummaryWriter(log_dir=os.path.join(Path().resolve().parents[1], f"logs\\UNet_trainer_{timestamp}"))

    best_v_loss = 1_000_000
    last_loss = 0
    losses = []

    for epoch in range(epochs):
        print(f"Epoch {epoch + 1}/{epochs}: ")

        model.train(True)
        running_loss = 0

        for i, data in enumerate(train_loader):
            data = data.to(model.device)

            optimizer.zero_grad()
            predictions = model(data)

            loss = loss_fn(predictions, data)
            loss.backward()

            optimizer.step()

            running_loss += loss.item()
            # if i % 50 == 49:
            # last_loss = running_loss  # / 50
            # tb_x = epoch * len(train_loader) + i + 1
            # writer.add_scalar('Loss/train', last_loss, tb_x)

        avg_loss = running_loss / len(train_loader)
        running_v_loss = 0.0
        model.eval()

        with torch.no_grad():
            for i, v_data in enumerate(val_loader):
                v_preds = model(v_data)
                v_loss = loss_fn(v_preds, v_data)
                running_v_loss += v_loss.item()

        avg_v_loss = running_v_loss / (epoch + 1)
        print(f"    | Loss train: {avg_loss}\n    | Loss validation: {avg_v_loss}")

        # writer.add_scalars('Training vs. Validation Loss', {"Training": last_loss, "Validation": avg_v_loss},
        #                    epoch + 1)
        # writer.flush()

        if avg_v_loss < best_v_loss:
            best_v_loss = avg_v_loss
            model_path = os.path.join(Path().resolve().parents[1], f"model_{timestamp}_{epoch}")
            torch.save(model.state_dict(), model_path)

        losses.append(avg_loss)

    return losses


def predict_one(model, x):
    x = x.to(model.device)
    return model(x).cpu().detach()