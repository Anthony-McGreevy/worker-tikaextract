/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.mcgreevy.worker.tikaextract.response;

import com.hpe.caf.api.CodecException;

/**
 *
 * @author mcgreeva
 */
public class MessageDispatchException extends Exception
{
    public MessageDispatchException(CodecException ex)
    {
        super(ex);
    }
    
}
