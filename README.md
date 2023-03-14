# IMAGE-TEXT CONVERTER


### This program will generate two output text files, one for english type and the other one for chinese type, that extracted from image files.
1. The program will find images files in "Digital SawitPRO - Test" Folder (Link - https://drive.google.com/drive/u/0/folders/1drMZ2klEhBYPSZSBIxYoV_aSSBEIe68y).
2. The image files will be downloaded and then saved to our local disk.
3. The program will extract a text from all downloaded image files.
4. The program will detect any chinese character on the text. If image file contains a chinese character, the output will belong to chinese.txt. The other hand, if image file don't contain any chinese character, it will belong to english.txt as the output.


Note:
- if you want to run this program, you need to add your Google Cloud Keys at top level (same as the output file level) and add your Google OAuth2 Keys on the resources folder (src/main/recsources). And also create "tokens" folder to store your credential once you are authorized. I also add how the structure file is in the structure_file.jpg