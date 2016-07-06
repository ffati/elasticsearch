/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.user.AnonymousUser;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportDeleteUserAction extends HandledTransportAction<DeleteUserRequest, DeleteUserResponse> {

    private final NativeUsersStore usersStore;

    @Inject
    public TransportDeleteUserAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                     IndexNameExpressionResolver indexNameExpressionResolver, NativeUsersStore usersStore,
                                     TransportService transportService) {
        super(settings, DeleteUserAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                DeleteUserRequest::new);
        this.usersStore = usersStore;
    }

    @Override
    protected void doExecute(DeleteUserRequest request, final ActionListener<DeleteUserResponse> listener) {
        final String username = request.username();
        if (ReservedRealm.isReserved(username)) {
            if (AnonymousUser.isAnonymousUsername(username)) {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is anonymous and cannot be deleted"));
                return;
            } else {
                listener.onFailure(new IllegalArgumentException("user [" + username + "] is reserved and cannot be deleted"));
                return;
            }
        } else if (SystemUser.NAME.equals(username)) {
            listener.onFailure(new IllegalArgumentException("user [" + username + "] is internal"));
            return;
        }

        usersStore.deleteUser(request, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean found) {
                listener.onResponse(new DeleteUserResponse(found));
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
