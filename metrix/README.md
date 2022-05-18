# Metrix tutorial
This tutorial aims to show how to perform OPF using Metrix on a small fictive network.

Translation types
Text translation
Source text
2,106 / 5,000
Translation results
---
author:
- Mathilde Bongrain
  title: "**TP Metrix - 6 node network - Statement**"
---

**Objectives of the practical work:** On a very simple network (6 workstations), to appropriate
Metrix settings and outputs, and knowing how to explain the
results obtained.

# Network presentation:

The network used for this lab consists of 6 stations, all connected
by two parallel lines with the same electrotechnical characteristics
(same resistors and same reactances for each line), as well as two
HVDC and a TD. It also features 4 groups and three loads.\
![image](images/reseau_6_noeuds_ss_HVDC.png)

# To get started:

1. Retrieve the files needed for the lab:

    - reseau_6noeuds.xiidm

   -time-series-tp.csv

   They can be downloaded from the directory of the community of
   MS studies: [link
   community](https://communautes.rte-france.com/sites/Etudes-Multi-Situations/SitePages/Accueil%20de%20la%20communaut%C3%A9.aspx)
   or available on the training USB key. The iidm network is
   that of the previous diagram and the chronicles file is presented
   thus :\

   Time step Version SE_L1 SO_G1 SO_G2 thresholdN thresholdAM
      -------------- --------- ------- ------- ------- ------ -- ---------
   T01 1,960 0 960 400 480
   T02 1,960,960 0 400,480
   T03 1,960,960 0 100,480

2. Create a new working folder and recreate the object
   multi-situations:

    - Create a \"TP_Metrix\" folder in your created working project
      the first day of training

    - (Re-)Import the iidm situation and the chronicles file.

    - Copy the groovy script \"Config_MS_reseau-6-noeuds.groovy\".

    - Create a \"multi-situation\" object with these elements then
      launch the analysis of the mapping to verify that each element has
      well received the good chronicle and that the final balance sheet is nil.

   In mapping output you should have the following network\
   ![image](images/result_mapping_ss_HVDC.png)
   More about this source text
   Source text required for additional translation information
   Send feedback
   Side panels
   5,000 character limit. Use the arrows to translate more.
