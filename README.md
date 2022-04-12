#Description
A small plugin that I used to test NMS with 
Mojang mappings, using the ``io.papermc.paperweight.userdev`` 
gradle plugin.

#Usage
Type ``/spawnnpc`` or ``/spawnnpc <name>`` to spawn an NPC.

The name used will attempt to grab a skin from a Minecraft account, if one exists.
If the name parameter is not provided, the NPC will be a clone of the user.

The NPC may wear armor as well, which will be equipped if the user has any on.

![img_1.png](img_1.png)
<sub><sup>Left Command (``/spawnnpc Notch``), Right Command (```/spawnnpc```)</sub></sup>