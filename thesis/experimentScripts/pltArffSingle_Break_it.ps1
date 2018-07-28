<#
path to arff data file (-data), label prefix in data file (-lprfx), path to the arff file to store the fmeasure results (-result), iterations (-itr), 
    1: hashed dimension (-hd)
    2: learner to resume (-r)
#>
$prop = "../data/learnerIds.properties"

function run($itr, $hd, $data, $lprfx, $result, $key){
    Try{
        Write-Host "Configuration confirmed. Running..." -ForegroundColor Green
        java -jar thesis-pltArff-0.0.40-SNAPSHOT-jar-with-dependencies.jar -itr $itr -hd $hd -wreg $true -data $data -lprfx $lprfx -result $result -prop $prop -key $key -resrel "Result-$key"
    }
    Catch{
        write-host “Caught an exception:” -ForegroundColor Red
        write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
        write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
    }
}

function resume($itr, $resumedLearnerId, $wres){
    java -jar thesis-pltArff-0.0.40-SNAPSHOT-jar-with-dependencies.jar -itr $itr -r $resumedLearnerId -wres $wres -data $data -lprfx $lprfx -result $result
}

function AddToPropFile($key,$val){    
     IF(!(Test-Path -path $prop)){
        $path = [System.IO.Path]::GetDirectoryName($prop)
        Write-Host $path
        New-Item -Force -ItemType directory -Path $path
        "$key=$val"|Set-Content $prop
     }ELSE{
        "$key=$val"|Add-Content $prop
     }
}

function ReadFromPropFile($key){
    $AppProps = convertfrom-stringdata (get-content $prop -raw)
    return $AppProps[$key]
}


Write-Host "STARTING APPLICATION..." -ForegroundColor Yellow


Try{
    $hd = [int](Read-Host -Prompt "Enter hashed dimension (int)")
    $data = Read-Host -Prompt "Enter the path to arff data file"
    $lprfx = Read-Host -Prompt "Enter the label prefix in data file" 
    $result = Read-Host -Prompt "Enter the path to the arff file to store the fmeasure results"
    
    Write-Host "Configuration: `n`tHashed Dimension: $hd, `n`tPath to arff data file: $data, `n`tLabel prefix in data file: $lprfx, `n`tPath to the arff file to store the fmeasure results: $result"

    $confirm = Read-Host -Prompt "Press 1 to confirm"
    IF($confirm -eq 1){
        $key = "pltArff-$hd-$(Get-Date -Format yyyy-MM-dd_HH:MM:ss)".ToLower()
        
        Write-Host $key

        if([string]::IsNullOrWhiteSpace($key)){
            throw "Invalid config : Reg key NULL"
        }

        AddToPropFile $Reg_key "INIT" 

        $chunkSize = 10#1000
        $totalCount = 30#25000
        $resumedLearnerId = ""

        FOR ($chunk = $chunkSize; $chunk -le $totalCount; $chunk += $chunkSize){
            $resumedLearnerId = ReadFromPropFile $key
            Write-Host $resumedLearnerId

            #first chunk
            IF($chunk -eq $chunkSize){                
                run $chunk $hd $data $lprfx $result $key
            }
            #last chunk
            ELSEIF($chunk -eq $totalCount){
                resume ([math]::Min($chunk, $totalCount)) $resumedLearnerId $true
            }
            #Chunks in the middle
            ELSE{               
                resume $chunk $resumedLearnerId $false
            }

            Write-Host "$Reg_key - Phase $($chunk/$chunkSize) over; $([math]::Min($chunk, $totalCount)) data processed." -ForegroundColor Yellow
            IF($chunk -ne $totalCount){
                Start-Sleep -s 120
            }
        }
    }
    ELSE{
        Write-Host "Configuration not confirmed. Aborted." -ForegroundColor Yellow
    }
}
Catch{
    write-host “Caught an exception:” -ForegroundColor Red
    write-host “Exception Type: $($_.Exception.GetType().FullName)” -ForegroundColor Red
    write-host “Exception Message: $($_.Exception.Message)” -ForegroundColor Red
}

Write-Host "ENDING APPLICATION.." -ForegroundColor Yellow