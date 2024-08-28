#!/bin/bash

start=$(date '+%s')

password="6mYN2VPj5HsPfByHrtKQySUx"
username="ecsonsftp"
host="10.210.20.120"

today_day=$(date -d "today" '+%d')
echo "Today's Day of the Month: $today_day"
yesterday_day=$(date -d "yesterday" '+%d')
echo "Yesterday's Day of the month: $yesterday_day"
yesterday_date=$(date -d "yesterday" '+%Y%m%d')
echo "Yesterday's date: $yesterday_date"
day_before_yesterday=$(date --date="${yesterday_date} - 1 day" +%d)
echo "Day before date: $day_before_yesterday"

current_time_minus_one=$(date -d "-1 hours" +'%H')
echo "Current time - 1: $current_time_minus_one"

if [ "$current_time_minus_one" == "23" ];
then
  day_t=$yesterday_day
  day_y=$day_before_yesterday
  cm_date=$(date -d "yesterday" '+%Y-%m-%d')
  pm_date=$(date -d "yesterday" '+%Y%m%d')
  cm_date_for_yesterday=$(date --date="${cm_date} - 1 day" '+%Y-%m-%d')
  pm_date_for_yesterday=$(date --date="${pm_date} - 1 day" '+%Y%m%d')
else
  day_t=$today_day
  day_y=$yesterday_day
  cm_date=$(date '+%Y-%m-%d')
  pm_date=$(date '+%Y%m%d')
  cm_date_for_yesterday=$(date -d "yesterday" '+%Y-%m-%d')
  pm_date_for_yesterday=$(date -d "yesterday" '+%Y%m%d')
fi

if [ "$today_day" == "01" ];
then
  month_c=$(date -d "today" '+%b')
  month_p=$(date -d "today - 1 day" '+%b')
  echo "Current month: $month_c"
  echo "Previous month: $month_p"
else
  month_c=$(date -d "today" '+%b')
  month_p=$month_c
  echo "Current month: $month_c"
  echo "Previous month: $month_p"
fi


required_files_path="/stubbed-enm/fls/eaamela/scripts/required_files.txt"
actual_path="/stubbed-enm/fls/eaamela/scripts/actual.txt"
itk_file_paths=("/sonsftp/PM/" "/sonsftp/CTR/" "/sonsftp/CM/")

#---------- function to retieve file names from ITK SFTP server
function get_file_names(){
  echo $1
  result=$(sshpass -p "$password" sftp -q -o StrictHostKeyChecking=no "$username"@"$host" << !
    cd $1
    ls -lt *.cpio.gz
    exit
!)
  echo "$result" > $actual_path
  if grep -q "CM" <<< "$1"; then
     echo "$result" | awk -v month="$month_p" -v t_day="$day_t" '($6 == month) && ($7 == t_day) {print $9}' > $required_files_path
    else
     echo "$result" | awk -v month_c="$month_c" -v month_p="$month_p" -v t_day="$day_t" -v y_day="$day_y" -v c_t="$current_time_minus_one" '($6 == month_p || $6 == month_c ) && (($7 == y_day && $8 >= "23:00") || ($7 == t_day && $8 <= c_t)) {print $9}' > $required_files_path
  fi
}


#----------- function to get the files (*.cpio.gz) based on file names
function get_files_and_verify(){

  rm -f remove_yesterday.txt
  cat $required_files_path > original.txt
  cat $required_files_path > filtered.txt
  while read file; do
    sshpass -p "$password" sftp -q -o StrictHostKeyChecking=no "$username"@"$host" << !
    cd $1
    get $file
!
    if grep -q "CM" <<< "$1"; then
     date_value="$cm_date"
     date_value_yesterday="$cm_date_for_yesterday"
    else
     date_value="$pm_date"
     date_value_yesterday="$pm_date_for_yesterday"
    fi
    count=$( gzip -cd $file | cpio -t | grep -c "$date_value")
    count_for_yesterday=$( gzip -cd $file | cpio -t | grep -c "$date_value_yesterday")
    if [ $count_for_yesterday -ne 0 ] && [ $count -ne 0 ];
    then
      echo "$file#" >> filtered_$day_y.txt
      echo "$file ---> both"
    elif [ $count_for_yesterday -ne 0 ];
    then
      echo "$file" >> filtered_$day_y.txt
      echo "$file" >> remove_yesterday.txt
      sed -i "/$file/d" filtered.txt
      echo "$file ---> yesterday"
    fi
  done < $required_files_path

  sort filtered.txt > sorted.txt

  while read -r file; do
  sshpass -p "$password" sftp -q -o StrictHostKeyChecking=no "$username"@"$host" << !
      cd $1
      rm $file
      rm $(echo "$file" | sed 's/.cpio.gz/.manifest.gz/g')
      exit
!
  done < remove_yesterday.txt

  while read -r file; do
  sshpass -p "$password" sftp -q -o StrictHostKeyChecking=no "$username"@"$host" << !
      cd $1
      rm $file
      rm $(echo "$file" | sed 's/.cpio.gz/.manifest.gz/g')
      exit
!
  done < sorted.txt
  cat sorted.txt >> filtered_$day_t.txt
}


# START
for path in ${itk_file_paths[@]}; do

  case $path in

    "/sonsftp/PM/")
      get_file_names $path
      cd /stubbed-enm/fls/eaamela/itk_files/PM/
      get_files_and_verify $path
      ;;

    "/sonsftp/CTR/")
      get_file_names $path
      cd /stubbed-enm/fls/eaamela/itk_files/CTR/
      get_files_and_verify $path
      ;;

    "/sonsftp/CM/")
      get_file_names $path
      cd /stubbed-enm/fls/eaamela/itk_files/CM/
      get_files_and_verify $path
      ;;

  esac
done

end=$(date '+%s')

if [ "$current_time_minus_one" == "01" ];
then
  today=$(date -d "today" '+%Y-%m-%d')
  execution_report_path="/stubbed-enm/fls/eaamela/scripts/execution_reports/execution_reports_$today.txt"
  echo "After Fetching ITK files from server" > $execution_report_path
  echo "Elapsed Time: $(($end-$start)) seconds" >> $execution_report_path
  echo "Fetching Files from ITK done." > /stubbed-enm/fls/eaamela/scripts/finish.txt
fi