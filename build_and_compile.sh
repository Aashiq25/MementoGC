bin/buildit -j /usr/lib/jvm/java-6-oracle localhost BaseBaseMementoV2 && \
cd ./dist/BaseBaseMementoV2_x86_64-linux && \
./rvm SampleOOM > "/mnt/hgfs/MementoGC/LogFiles/Nov_21_Collection_Exp_1_$1.txt"
