**DISCLAIMER**: Solved after competition ended.

>Android is all about the desserts, but can you come up with the secret recipe to cook up a flag?<br><br>[food.apk](https://capturetheflag.withgoogle.com/attachment/9fbe2bb7e74937796b6d7eb734cdde808f3cecb7e8c4c6dcd066fbfe477e45b3)

If the link above doesn't work to download the apk, I uploaded the apk to this folder as [food.apk](food.apk). 

The first step with our apk is to decompile it. I normally use this site: http://www.javadecompilers.com/apk. You can view the files from the decompiled apk in the [food_source_from_JADX](food_source_from_JADX) folder.

Let's take a look at the [FoodActivity.java](/food_source_from_JADX/com/google/ctf/food/FoodActivity.java) file


Our flag is `CTF{bacon_lettuce_tomato_lobster_soul}`
