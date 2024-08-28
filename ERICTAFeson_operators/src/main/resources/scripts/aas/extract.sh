#!/bin/bash

start=$(date '+%s')

today_date=$(date -d "today" '+%Y%m%d')
echo "Today's date: $today_date"
yesterday_day=$(date -d "yesterday" '+%d')
echo "Yesterday's Day of the month: $yesterday_day"

yesterday_date=$(date -d "yesterday" '+%Y%m%d')
yesterday_date_for_cm=$(date -d "yesterday" '+%Y-%m-%d')

today=$(date -d "today" '+%Y-%m-%d')
TODAY_DATE=$(date -d "today" '+%Y-%m-%d')

declare -A required_files_path
declare -A extracted_file_paths

sudo find /stubbed-enm/fls/eaamela/files/cucp -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/eaamela/files/du -type f -exec rm -rf {} \;

required_files_path=( ["PM"]="/stubbed-enm/fls/eaamela/itk_files/PM/filtered_$yesterday_day.txt" ["CTR"]="/stubbed-enm/fls/eaamela/itk_files/CTR/filtered_$yesterday_day.txt" ["CM"]="/stubbed-enm/fls/eaamela/itk_files/CM/filtered_$yesterday_day.txt")
extracted_file_paths=( ["PM"]="/stubbed-enm/fls/files/pm_statistical/radionode/0000" ["CTR_CUCP"]="/stubbed-enm/fls/eaamela/files/cucp/" ["CTR_DU"]="/stubbed-enm/fls/eaamela/files/du/" ["PM_ERBS"]="/stubbed-enm/fls/files/pm_statistical/erbs/0000" ["CM"]="/bulk/export" )

count_corrupted_files=0

#------------ function to unzip (*.cpio.gz) and extract contents
function extract(){
while read c_file;
do
    name=$c_file
    c_file=$( echo "$name" | sed 's/#//g' )
    case $1 in

      "PM")
         gzip -cd $c_file | cpio --quiet -ivdum
         for file in ./*/*.gz;
         do
             if ! gzip -t $file; then
               echo 'file is corrupt: $file'
               rm -r $file
               ((count_corrupted_files+=1))
             fi
         done;
         find . -type f -name "*V0036604E088441*" -exec sudo cp "{}" ${extracted_file_paths["$1_ERBS"]}  \;
         find . -type f \( -iname "*_statsfile.xml.gz" ! -iname "*V0036604E088441*" \) -exec sudo cp "{}" ${extracted_file_paths[$1]}  \;
         rm -r */
        ;;

      "CTR")
        mkdir -p missing
        gzip -cd $c_file | cpio --quiet -ivdum
        find . -type f -iname "*.gpb" -exec sh -c 'basename {}; gzip {}; cp {}.gz /stubbed-enm/fls/eaamela/itk_files/CTR/missing/; gunzip {}' \;
        find . -type f -name "*_DU0*.gpb.gz" -exec sudo cp "{}" ${extracted_file_paths["$1_DU"]}  \;
        find . -type f -name "*_CUCP0*.gpb.gz" -exec sudo cp "{}" ${extracted_file_paths["$1_CUCP"]}  \;
        rm -r */
        rm *.gpb.gz
        rm *.bin.gz
        ;;
     "CM")
        date_diff=1
        files_present_in_archive=$( gzip -cd $c_file | cpio -t)
        while true
        do
                DAY_BEHIND=$(date --date="${TODAY_DATE} - ${date_diff} day" +%Y-%m-%d)
                file_name=$( echo $files_present_in_archive | grep "$DAY_BEHIND")
          if [ ! -z "$file_name" ];
          then
            break
          fi
          ((date_diff+=1))
        done
        gzip -cd $c_file | cpio --quiet -ivdum
        find . -type f -name "${file_name}" -exec gunzip "{}" \;
        find . -type f -name '*_EDFF_ITK-EXPORT_AAS_POC_*.txt' -exec sudo cp "{}" ${extracted_file_paths[$1]} \;
        find . -type f -name '*_EDFF_ITK-EXPORT_AAS_POC_*.txt' -exec sh -c 'name=$(echo {} | cut -c3-) ;  mv {} RET_5G_$name' \;
        find . -type f -name 'RET_5G_*.txt' -exec sudo mv "{}" ${extracted_file_paths[$1]} \;
        ;;
    esac

    if ! grep -q "#" <<< "$name"; then
      rm -r $c_file
    fi
done <$2
#rm -f $2
}


function cleanup_and_change_permission(){

      sudo find $1 -type f ! \( -iname "*${yesterday_date}*" -o -name "*${yesterday_date_for_cm}*" \) -exec rm -rf {} \;
      sudo find $1 -type f -exec chmod 664 {} \;
      sudo find $1 -type f -exec chown cloud-user:cloud-user {} \;
}


#check if fetching is finished
while [ ! -f /stubbed-enm/fls/eaamela/scripts/finish.txt ]
do
  sleep 2
  echo "Fetching files from ITK is not complete !!!"
done

cd /stubbed-enm/fls/eaamela/scripts
rm finish.txt
rm required_files.txt

sudo find /stubbed-enm/fls/files/pm_statistical/radionode/0000 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_statistical/erbs/0000 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0000 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_celltrace_du/radionode/0000 -type f -exec rm -rf {} \;
sudo find "/bulk/export" -type f -exec rm -rf {} \;


# START
for key in ${!required_files_path[@]}; do

  case $key in

   "PM")
      cd /stubbed-enm/fls/eaamela/itk_files/PM/
      ;;
   "CTR")
      cd /stubbed-enm/fls/eaamela/itk_files/CTR/
      ;;
   "CM")
      cd /stubbed-enm/fls/eaamela/itk_files/CM/
      ;;
  esac

  extract $key ${required_files_path[$key]}
done


for key in ${!extracted_file_paths[@]}; do
  cleanup_and_change_permission ${extracted_file_paths[$key]}
done


end=$(date '+%s')

execution_report_path="/stubbed-enm/fls/eaamela/scripts/execution_reports/execution_reports_$today.txt"

echo -e "\n---------------------------------------------------" >> $execution_report_path
echo -e "\nAfter extracting files" >> $execution_report_path
echo -e "\nElapsed Time: $(($end-$start)) seconds" >> $execution_report_path
echo -e "\nNumber of Statistical files from Radionode: $(ls /stubbed-enm/fls/files/pm_statistical/radionode/0000 | wc -l)" >> $execution_report_path
echo -e "\nNumber of Statistical files from ERBS node: $(ls /stubbed-enm/fls/files/pm_statistical/erbs/0000 | wc -l)" >> $execution_report_path
echo -e "\nNumber of Celltrace CUCP files: $(ls /stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0000 | wc -l)" >> $execution_report_path
echo -e "\nNumber of Celltrace DU files: $(ls /stubbed-enm/fls/files/pm_celltrace_du/radionode/0000 | wc -l)" >> $execution_report_path
echo -e "\nNumber of corrupted files: $count_corrupted_files" >> $execution_report_path

analysis_report_path="/stubbed-enm/fls/eaamela/scripts/analysis/analysis_$today.txt"
#Analysis from the logs
echo -e "\nStats file counts for the execution on $today\n\n" > $analysis_report_path
grep gz /stubbed-enm/fls/eaamela/scripts/extract_logs.txt | grep stats | awk '{split($1,a,","); print a[3]}'| sort | uniq -c >> $analysis_report_path
echo -e "\nDU0 file counts for the execution on $today\n\n" >> $analysis_report_path
grep gz /stubbed-enm/fls/eaamela/scripts/extract_logs.txt | grep DU0 | awk '{split($1,a,","); print a[3]}'| sort | uniq -c >> $analysis_report_path
echo -e "\nCUCP file counts for the execution on $today\n\n" >> $analysis_report_path
grep gz /stubbed-enm/fls/eaamela/scripts/extract_logs.txt | grep -i CUCP | awk '{split($1,a,","); print a[3]}'| sort | uniq -c >> $analysis_report_path


echo "Fetching and Extracting files from ITK done. Restarting the Stubbed ENM" > /stubbed-enm/fls/eaamela/scripts/finish.txt

declare -A nodes_to_extract
declare -A extracted_file_paths_filtered

#Filtering out the nodes
nodes_to_extract=( ["CTR_CUCP"]="/stubbed-enm/fls/eaamela/files/cucp/" ["CTR_DU"]="/stubbed-enm/fls/eaamela/files/du/" )
extracted_file_paths_filtered=( ["CTR_CUCP"]="/stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0000" ["CTR_DU"]="/stubbed-enm/fls/files/pm_celltrace_du/radionode/0000" )
nodes_file="/stubbed-enm/fls/eaamela/scripts/filternodes/nodes.txt"

for key in ${!nodes_to_extract[@]}; do
  echo "Filtering files for $key:"

  while read node_name;
  do
    find ${nodes_to_extract[$key]} -type f -iname "*${node_name}*" -exec sudo cp "{}" ${extracted_file_paths_filtered["$key"]}  \;
    sudo find ${extracted_file_paths_filtered["$key"]} -type f -exec chmod 664 {} \;
    sudo find ${extracted_file_paths_filtered["$key"]} -type f -exec chown cloud-user:cloud-user {} \;
  done <$nodes_file
done

echo -e "\n---------------------------------------------------" >> $execution_report_path
echo -e "\nNumber of Celltrace CUCP files: $(ls /stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0000 | wc -l)" >> $execution_report_path
echo -e "\nNumber of Celltrace DU files: $(ls /stubbed-enm/fls/files/pm_celltrace_du/radionode/0000 | wc -l)" >> $execution_report_path

#Splitting the event files (CUCP & DU) into 3 folders 0000, 0015, 0030

sudo find /stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0015 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_celltrace_cucp/radionode/0030 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_celltrace_du/radionode/0015 -type f -exec rm -rf {} \;
sudo find /stubbed-enm/fls/files/pm_celltrace_du/radionode/0030 -type f -exec rm -rf {} \;

HOURS_0015=(08 09 10 11 12 13 14 15)
HOURS_0030=(16 17 18 19 20 21 22 23)
MINUTES=(00 15 30 45)

events_path=("/stubbed-enm/fls/files/pm_celltrace_cucp/radionode" "/stubbed-enm/fls/files/pm_celltrace_du/radionode")

echo "Splitting CUCP files"
for path in ${events_path[@]};
  do

  cd $path/0000
  pwd
  for(( i=0; i<${#HOURS_0015[@]}; i++))
  do
        for(( j=0; j<${#MINUTES[@]}; j++))
    do
      if [ ${MINUTES[$j]} -eq 45 ] && [ ${HOURS_0015[$i]} -ne ${HOURS_0015[-1]} ]
      then
          find . -type f -iname "*${HOURS_0015[$i]}${MINUTES[$j]}*${HOURS_0015[$i+1]}${MINUTES[0]}*" -exec mv {} $path/0015 \;
      elif [ ${MINUTES[$j]} -eq 45 ] && [ ${HOURS_0015[$i]} -eq ${HOURS_0015[-1]} ]
      then
          find . -type f -iname "*${HOURS_0015[$i]}${MINUTES[$j]}*${HOURS_0030[0]}${MINUTES[0]}*" -exec mv {} $path/0015 \;
      else
          find . -type f -iname "*${HOURS_0015[$i]}${MINUTES[$j]}*${HOURS_0015[$i]}${MINUTES[$j+1]}*" -exec mv {} $path/0015 \;
      fi
    done
  done
done

echo "Splitting DU files"
for path in ${events_path[@]};
  do

  cd $path/0000
  pwd
  for(( i=0; i<${#HOURS_0030[@]}; i++))
  do
        for(( j=0; j<${#MINUTES[@]}; j++))
    do
      if [ ${MINUTES[$j]} -eq 45 ] && [ ${HOURS_0030[$i]} -ne ${HOURS_0030[-1]} ]
      then
          find . -type f -iname "*${HOURS_0030[$i]}${MINUTES[$j]}*${HOURS_0030[$i+1]}${MINUTES[0]}*" -exec mv {} $path/0030 \;
      elif [ ${MINUTES[$j]} -eq 45 ] && [ ${HOURS_0030[$i]} -eq ${HOURS_0030[-1]} ]
      then
          find . -type f -iname "*${HOURS_0030[$i]}${MINUTES[$j]}*00${MINUTES[0]}*" -exec mv {} $path/0030 \;
      else
          find . -type f -iname "*${HOURS_0030[$i]}${MINUTES[$j]}*${HOURS_0030[$i]}${MINUTES[$j+1]}*" -exec mv {} $path/0030 \;
      fi
    done
  done
done

# Restarting the Stubbed ENM
pid=$(sudo ps -ef | grep "java" | awk '{print $2}')

for id in $pid;
do
  echo "$id" | sudo xargs kill -9
done;

cd /home/cloud-user/wildfly-13.0.0.Final
sudo ./bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 &