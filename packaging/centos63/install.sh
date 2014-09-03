#!/bin/bash -e

function usage() {
 echo ""
 echo "./install.sh -m | --install-management (Install the Management Server)";
 echo "./install.sh -a | --install-agent (Install the Agent)";
 echo "./install.sh -b | --install-baremetal (Install BareMetal Agent)";
 echo "./install.sh -s | --installus-user (Install the Usage Monitor)";
 echo "./install.sh -d | --installdb-database (Install the database server (from distribution's repo))";
 echo "./install.sh -l | --install-mysql (Install the MySQL 5.1.58 (only for CentOS5.x, Rhel6.x naturally has higher version MySql))";
 echo "./install.sh --install-management --install-agent --install-baremetal --install-user --install-database --install-mysql (Installing everything in one short using long option);"
 echo "./install.sh -m -a -b -s -d -l (Installing everything in one short using short option);"
 echo "./install.sh -u|--upgrade cloudstack|cdsk (Upgrade the CloudPlatform packages installed on this machine)";
 echo "./install.sh -r|--remove db (Remove the MySQL server (will not remove the MySQL databases))";
 echo "./install.sh -r cdsk|cloudstack (Removing all CloudPlatform packages on this machine)";
 echo "./install.sh -h | --help (To view the Man page)";
 echo ""
 exit 1;
 }

. scripts/install_mysql5158_rpm.sh

function cleanup() {
    rm -f /etc/yum.repos.d/cloud-temp.repo || true
}

function setuprepo() {
    pathtorepo=`pwd`
    echo "Setting up the temporary repository..." >&2
    echo \
"[cloud-temp]
baseurl=file://$pathtorepo
gpgcheck=0
enabled=1
name=CloudStack temporary repository
" > /etc/yum.repos.d/cloud-temp.repo

    echo "Cleaning Yum cache..." >&2
    rm -rf /var/cache/yum/x86_64/6Server/cloud-temp/
    yum clean expire-cache || true
}

function installed() {
    rpm -q "$@" > /dev/null 2>&1 || return $?
}

function doinstall() {
    yum install "$@" || return $?
}

function doinstallauto() {
    yum install "$@" -y || return $?
}
    
function doupdate() {
    yum update --enablerepo='cloud-temp' 'cloudstack-*' || return $?
    #rpm -Uvh --force cloud-scripts-*.rpm
}
    
function doremove() {
    yum remove "$@" || return $?
}

set +e
[ `whoami` != 'root' ] && echo "This script must run as root" && exit 1
uname -a | grep 'x86_64' >/dev/null
[ "$?" -ne 0 ] && echo "CloudStack only supports x86_64 platform now" && exit 1
set -e

trap "cleanup" INT TERM EXIT

cd `dirname "$0"`

installms="    M) Install the Management Server   
"
installag="    A) Install the Agent
"
installbm="    B) Install BareMetal Agent
"
installus="    S) Install the Usage Monitor
"
installdb="    D) Install the database server (from distribution's repo)      
"
installmysql5158="    L) Install the MySQL 5.1.58 (only for CentOS5.x, Rhel6.x naturally has higher version MySql)
"
quitoptio="    Q) Quit
"
unset removedb
unset upgrade
unset remove

if installed cloudstack-management || installed cloudstack-agent || installed cloudstack-usage || installed cloudstack-baremetal-agent; then
    upgrade="    U) Upgrade the CloudPlatform packages installed on this computer
"
    remove="    R) Stop any running CloudPlatform services and remove the CloudPlatform packages from this computer
"
fi
if installed cloudstack-management ; then
    unset installms
fi
if installed cloudstack-agent ; then
    unset installag
fi
if installed cloudstack-usage ; then
    unset installus
fi
mysql_note=""

if installed mysql-server ; then
    unset installdb
    unset installmysql5158
    removedb="    E) Remove the MySQL server (will not remove the MySQL databases)
"
    mysql_note="3.We detect you already have MySql server installed, you can bypass mysql install chapter in CloudPlatform installation guide.
        Or you can use E) to remove current mysql then re-run install.sh selecting D) to reinstall if you think existing MySql server has some trouble.
        For MySql downloaded from community, the script may not be able to detect it."
fi

if installed MySQL-server-community-5.1.58 || installed MySQL-client-community-5.1.58; then
    unset installmysql5158
    unset installdb
fi

if [ $# -lt 1 ] ; then

setuprepo

	read -p "Welcome to the CloudStack Installer.  What would you like to do?

	NOTE:	For installing KVM agent, please setup EPEL<http://fedoraproject.org/wiki/EPEL> yum repo first;
		For installing CloudPlatform on RHEL6.x, please setup distribution yum repo either from ISO or from your registeration account.
		$mysql_note

$installms$installag$installbm$installus$installdb$upgrade$remove$removedb$installmysql5158$quitoptio > " installtype

fi

if [ "$installtype" == "q" -o "$installtype" == "Q" ] ; then

    true

	elif [ "$installtype" == "m" -o "$installtype" == "M" ] ; then
		echo "Installing the Management Server..." >&2
		doinstall cloudstack-management
		true

	elif [ "$installtype" == "a" -o "$installtype" == "A" ] ; then
		echo "Installing the Agent..." >&2
		if doinstall cloudstack-agent; then
                        modprobe kvm
                        modprobe kvm_intel > /dev/null 2>&1 
                        modprobe kvm_amd > /dev/null 2>&1
                        if [[ `cat /etc/redhat-release` =~ "6.5" ]]; then
                           yum localinstall 6.5/ccp-qemu-img* -y
                        fi
			echo "Agent installation is completed, please add the host from management server" >&2
		else
			true
		fi
		
	elif [ "$installtype" == "b" -o "$installtype" == "B" ] ; then
		echo "Installing the BareMetal Agent..." >&2
		doinstall cloudstack-baremetal-agent
		true

	elif [ "$installtype" == "s" -o "$installtype" == "S" ] ; then
		echo "Installing the Usage Server..." >&2
		doinstall cloudstack-usage cloud-premium
		true

	elif [ "$installtype" == "d" -o "$installtype" == "D" ] ; then
		echo "Installing the MySQL server..." >&2
    	        if [[ `cat /etc/redhat-release` =~ "release 7.0" ]]; then
            	    mysql_type=mysql-community-server
        	    if [[ `curl -s --head -w %{http_code} http://dev.mysql.com/ -o /dev/null` == "200" ]]; then
                       echo "Mysql repo http://dev.mysql.com server exist"
        	    else 
            	       echo "Unable to acccess http://dev.mysql.com/, mysql repo not configured, please configure mysql server repo" 
                    fi
     	        else 
        	    mysql_type=mysql-server
         fi
		if doinstall $mysql_type ; then
			#/sbin/chkconfig --add mysqld 
			/sbin/chkconfig --level 345 mysqld on
        if /sbin/service mysqld status > /dev/null 2>&1 ; then
            echo "Restarting the MySQL server..." >&2
            /sbin/service mysqld restart # mysqld running already, we restart it
        else
            echo "Starting the MySQL server..." >&2
            /sbin/service mysqld start   # we start mysqld for the first time
        fi
		else
			true
		fi

	elif [ "$installtype" == "u" -o "$installtype" == "U" ] ; then
		echo "Updating the CloudPlatform and its dependencies..." >&2
		doupdate


	elif [ "$installtype" == "r" -o "$installtype" == "R" ] ; then
		echo "Removing all CloudPlatform packages on this computer..." >&2
		doremove 'cloudstack-*'

	elif [ "$installtype" == "e" -o "$installtype" == "E" ] ; then
		echo "Removing the MySQL server on this computer..." >&2
		doremove 'mysql-server'
	elif [ "$installtype" == "l" -o "$installtype" == "L" ] ; then
		echo "Installing the MySQL server 5.1.58 on this computer ..." >&2
		install_mysql

	
#Start of auto detect options

elif [ $# -gt 0 ] ; then

function commonUpgrade () {
	#Common Upgrade section
		if installed cloud-client || installed cloud-agent || installed cloud-usage || installed cloud-baremetal-agent; then
			echo "***** Updating the CloudPlatform and its dependencies *****"
			doupdate
		fi
}	

function commonRemoval () {
#Common Remove section
	if [ "$remove" == "cdsk" ] || [ "$remove" == "cloudstack" ]; then
		if installed cloud-client || installed cloud-agent || installed cloud-usage || installed cloud-baremetal-agent; then
		echo "***** Removing all CloudPlatform packages on this machine *****"
		doremove 'cloud-*'
		else
			echo "CloudStack is not installed on this machine" 
		fi
	fi

	if [ "$remove" == "db" ]; then
		if installed mysql-server || installed MySQL-server-community-5.1.58 || installed MySQL-client-community-5.1.58; then
		echo "***** Removing the MySQL server on this computer *****"
		doremove 'mysql-server'
		else
			echo "mysql-server is not installed on this machine"
		fi
	fi

}	

SHORTOPTS="hmabsdlu:r:" 
LONGOPTS="help,install-management,install-agent,install-baremetal,install-usage,install-database,install-mysql,upgrade:,remove:" 

ARGS=$(getopt -s bash -u -a --options $SHORTOPTS  --longoptions $LONGOPTS --name $0 -- "$@" ) 

eval set -- "$ARGS"

setuprepo

while [ $# -gt 0 ] ; do
 case "$1" in
  -h | --help)
     usage
     exit 0
     ;;
  -m | --install-management)
     echo "***** Installing the Management Server *****"
	 doinstallauto cloudstack-management
	 true
	 shift
     ;;
  -a | --install-agent)
     echo "***** Installing the Agent *****"
		if doinstallauto cloudstack-agent; then
                        modprobe kvm
                        modprobe kvm_intel
			echo "Agent installation is completed, please add the host from management server"
		else
			true
		fi	
	shift
     ;;
  -b | --install-baremetal)
     echo "***** Installing the BareMetal Agent *****"
	 doinstallauto cloudstack-baremetal-agent
	 true
     shift
     ;;
  -s | --install-user)
     echo "***** Installing the Usage Server *****"
	 doinstallauto cloudstack-usage cloud-premium
	 true
     shift
     ;;
  -d | --install-database)
     echo "***** Installing the MySQL server *****"
    	 if [[ `cat /etc/redhat-release` =~ "release 7.0" ]]; then
            	mysql_type=mysql-community-server
        	if [[ `curl -s --head -w %{http_code} http://dev.mysql.com/ -o /dev/null` == "200" ]]; then
                   echo "Mysql repo http://dev.mysql.com server exist"
        	else 
            	   echo "Unable to acccess http://dev.mysql.com/, mysql repo not configured, please configure mysql server repo" 
                fi
     	 else 
        	mysql_type=mysql-server
         fi
     
         if doinstallauto $mysql_type ; then
                        #/sbin/chkconfig --add mysqld
			/sbin/chkconfig --level 345 mysqld on
			if /sbin/service mysqld status > /dev/null; then
				echo "Restarting the MySQL server..."
				/sbin/service mysqld restart # mysqld running already, we restart it
			else
				echo "Starting the MySQL server..."
				/sbin/service mysqld start   # we start mysqld for the first time
			fi
		else
			true
		fi	
     shift
     ;;
  -l | --install-mysql)
     echo "***** Install the MySQL 5.1.58 (only for CentOS5.x, Rhel6.x naturally has higher version MySql) *****"
	 install_mysql
     shift
     ;;
  -u | --upgrade)
     upgrade=$2
	 if [ "$upgrade" == "cdsk" -o "$upgrade" == "cloudstack" ] ; then
		commonUpgrade
	 else
		echo "Error: Incorrect value provided for the CloudPlatform upgrade, please provide proper value, see help ./install.sh --help|h ..."
	 exit 1
	 fi
     shift 2
     ;;
  -r | --remove)
     remove=$2
     if [ "$remove" == "cdsk" -o "$remove" == "cloudstack" -o "$remove" == "db" ] ; then
		commonRemoval
	 else
		echo "Error: Incorrect value provided for the removal , please provide proper value, see help ./install.sh --help|h ..."
	 exit 1
	 fi
     shift 2
     ;;	 
  --) 
     shift 
     break 
     ;; 
  -*)
    echo "Unrecognized option..."
	usage
	exit 1
	;; 
   *) 
     shift 
     break 
     ;;  
  esac
done
	
	
#End of auto detect options

else
    echo "Incorrect choice.  Nothing to do." >&2
	echo "Please, execute just ./install.sh or ./install.sh --help for more help"
fi

echo "Done" >&2
cleanup
