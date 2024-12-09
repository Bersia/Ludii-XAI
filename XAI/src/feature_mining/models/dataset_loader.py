import os
import torch

from skimage import io


class TreemapsDataset(torch.utils.data.Dataset):
    def __init__(self, image_folder, transform=None):
        self.image_dir = image_folder
        self.transform = transform

    #     https://stackoverflow.com/questions/2632205/how-to-count-the-number-of-files-in-a-directory-using-python
    def __len__(self):
        files_in_dir = next(os.walk(self.image_dir))[2]
        return len(files_in_dir)

    def __getitem__(self, item):
        if torch.is_tensor(item):
            item = item.tolist()

        image = io.imread(os.path.join(self.image_dir, f"treemap_{item}.png"))

        if self.transform:
            image = self.transform(image)

        return image
