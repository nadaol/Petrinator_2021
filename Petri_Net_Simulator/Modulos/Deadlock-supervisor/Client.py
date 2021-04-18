
from bs4 import BeautifulSoup
import re
import os
import numpy as np
import re
import sys
import html_txt_all as hta
import filter_data as filterdata
import new_red
import arcs as arcosrdp 
import shutil
import socket as sk
import time
'''
import html_txt_all as hta
import filter_data as filterdata
import new_red
import arcs as arcosrdp 
'''
def main():
    comunicacion()
    exit(0)

def comunicacion():

    '''
    #!/usr/bin/python
    port = int(sys.argv[1])
    path = "python "+os.path.dirname(os.path.realpath(__file__)) + "/Client2.py " + str(port)
    #print(path)
    os.system (path)

    '''
    length_of_message = int.from_bytes(sCliente.recv(2), byteorder='big')
    msg = sCliente.recv(length_of_message).decode("UTF-8")

    if (msg == "1"):
            
        respuesta = "lei el mensaje que fue 1 bestia pop<br> son dos sifones <br>-id:0<br>-id:1".encode("UTF-8")
        sCliente.send(len(respuesta).to_bytes(2, byteorder='big'))
        sCliente.send(respuesta)
        
    sCliente.close()
    return

host = "127.0.0.1"
port = int(sys.argv[1])
#print(port)
sCliente =  sk.socket()
sCliente.connect((host, port))
main()