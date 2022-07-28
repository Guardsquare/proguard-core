/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package proguard.analysis.cpa.interfaces;

import proguard.classfile.Signature;

import java.util.List;

/**
 * An interface for {@link TransferRelation}s that depend on the {@link proguard.analysis.cpa.defaults.Cfa} location for which the successor can be defined for the entering edges of the current location.
 *
 * @author James Hamilton
 */

public interface ProgramLocationDependentBackwardTransferRelation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
        extends ProgramLocationDependentTransferRelation<CfaNodeT, CfaEdgeT, SignatureT>
{
    @Override
    default List<CfaEdgeT> getEdges(ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> state) {
        return state.getProgramLocation().getEnteringEdges();
    }
}
