import os
import torch
import matplotlib.pyplot as plt

from torch import nn
from pathlib import Path
from torchvision import transforms, utils

from feature_mining.models.dataset_loader import TreemapsDataset
from feature_mining.models.unet import NoPoolUNet, train_model, predict_one, ScaledUNet, StandardUNet

if __name__ == '__main__':
    # parameters
    batch_size = 1

    # set up CUDA device
    device = ("cuda" if torch.cuda.is_available()
              else "cpu")
    print(f"Using {device} device.")

    # data loaders
    transform = transforms.ToTensor()
    parent = Path().resolve().parents[2]

    outlines_dataset_train = TreemapsDataset(image_folder=os.path.join(parent, "outputs\\treemaps\\outlines\\train\\all\\test"),
                                             transform=transform)
    outlines_dataset_val = TreemapsDataset(image_folder=os.path.join(parent, "outputs\\treemaps\\outlines\\validation"),
                                           transform=transform)

    no_outlines_dataset_train = TreemapsDataset(image_folder=os.path.join(parent, "outputs\\treemaps\\no_outline\\train\\all"),
                                             transform=transform)
    no_outlines_dataset_val = TreemapsDataset(
        image_folder=os.path.join(parent, "outputs\\treemaps\\no_outline\\validation"),
        transform=transform)

    outlines_dataloader_train = torch.utils.data.DataLoader(outlines_dataset_train, batch_size=batch_size, shuffle=True)
    outlines_dataloader_val = torch.utils.data.DataLoader(outlines_dataset_val, batch_size=batch_size, shuffle=False)

    no_outlines_dataloader_train = torch.utils.data.DataLoader(no_outlines_dataset_train, batch_size=batch_size, shuffle=True)
    no_outlines_dataloader_val = torch.utils.data.DataLoader(no_outlines_dataset_val, batch_size=batch_size, shuffle=False)

    # set up UNet
    outlines_unet = StandardUNet(device).to(device)
    no_outlines_unet = StandardUNet(device).to(device)

    loss_fn = nn.CrossEntropyLoss()
    outlines_optimizer = torch.optim.Adam(outlines_unet.parameters(), lr=1e-3)
    no_outlines_optimizer = torch.optim.Adam(no_outlines_unet.parameters(), lr=1e-3)

    outlines_history = train_model(outlines_unet, outlines_dataloader_train, outlines_dataloader_val, 10, outlines_optimizer, loss_fn)
    no_outlines_history = train_model(no_outlines_unet, no_outlines_dataloader_train, no_outlines_dataloader_val, 10, no_outlines_optimizer, loss_fn)

    # plot results
    plt.figure()
    plt.plot(outlines_history)
    plt.plot(no_outlines_history)
    plt.title("Training Loss U-Net")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.legend(["Outlines", "No Outlines"])
    plt.savefig(os.path.join(parent, "models\\training_loss.png"))

    # visual evaluation
    for i_batch, sample_batched in enumerate(outlines_dataloader_train):
        plt.figure()
        plt.subplot(1, 2, 1)
        grid = utils.make_grid(sample_batched, padding=25, pad_value=255)
        # break
        plt.imshow(grid.numpy().transpose((1, 2, 0)))
        plt.title("Original Image")
        plt.axis('off')
        plt.ioff()

    # for i, sample in enumerate(outlines_dataloader_train):
        output = predict_one(outlines_unet, sample_batched)
        plt.subplot(1, 2, 2)
        grid = utils.make_grid(output, padding=25, pad_value=255)
        plt.imshow(grid.numpy().transpose((1, 2, 0)))
        plt.title("Reconstructed Image")
        plt.axis('off')
        plt.ioff()
        plt.savefig(os.path.join(parent, f"models\\outlines_result_{i_batch}.png"))

    for i_batch, sample_batched in enumerate(no_outlines_dataloader_train):
        plt.figure()
        plt.subplot(1, 2, 1)
        grid = utils.make_grid(sample_batched, padding=25, pad_value=255)
        # break
        plt.imshow(grid.numpy().transpose((1, 2, 0)))
        plt.title("Original Image")
        plt.axis('off')
        plt.ioff()

    # for i, sample in enumerate(no_outlines_dataloader_train):
        output = predict_one(no_outlines_unet, sample_batched)
        plt.subplot(1, 2, 2)
        grid = utils.make_grid(output, padding=25, pad_value=255)
        plt.imshow(grid.numpy().transpose((1, 2, 0)))
        plt.title("Reconstructed Image")
        plt.axis('off')
        plt.ioff()
        plt.savefig(os.path.join(parent, f"models\\no_outlines_result_{i_batch}.png"))

    # examine latent space
    # for i, sample in enumerate(outlines_dataloader_train):
    #     outlines_features = outlines_unet.get_features(sample)
    #
    # for i, sample in enumerate(no_outlines_dataloader_train):
    #     no_outlines_features = no_outlines_unet.get_features(sample)
    #
    # print(f'With Outlines: {outlines_features.shape}')
    # print(f'Without Outlines: {no_outlines_features.shape}')
